# PST 数据层详细设计

> **L2 详设** -- 基于 L1 架构产出 `data-flow.md` 和决策文档细化。PST 领域无新增数据库表，数据层聚焦于微信 API 交互和内存缓存管理。

## 1. 设计概述

PST 领域的数据层特殊性：
- **无新增数据库表**：小程序码生成不涉及持久化存储
- **两个内存缓存管理器**：WxAccessTokenManager（access_token 缓存）+ WxacodeCacheManager（小程序码缓存）
- **外部 API 依赖**：微信开放平台 API（获取 access_token + 生成小程序码）
- **复用已有数据**：Activity 实体（activityMapper.selectById）、StudioConfig（画室名称）

---

## 2. WxAccessTokenManager 设计

### 2.1 职责

管理微信 access_token 的获取和缓存。access_token 是调用微信服务端 API 的凭证，有效期 2 小时。

**需求来源**：决策 5（微信 access_token 管理）、data-flow.md 2.2 节

### 2.2 数据结构

```
Class: WxAccessTokenManager
  Annotations: @Component

  Fields:
    appid       : String       -- 从 application.yml wx.appid 注入（@Value）
    secret      : String       -- 从 application.yml wx.secret 注入（@Value）
    cachedToken : String       -- 缓存的 access_token 值（volatile）
    expireTime  : long         -- token 过期时间戳（毫秒）（volatile）
    restTemplate: RestTemplate -- HTTP 客户端（注入或内部创建）

  Constants:
    TOKEN_URL   = "https://api.weixin.qq.com/cgi-bin/token"
    ADVANCE_REFRESH_SECONDS = 300  -- 提前 5 分钟刷新，避免边界过期
```

### 2.3 核心方法

```
METHOD getAccessToken() -> String:
  /**
   * 获取有效的 access_token。
   * 线程安全：使用 synchronized 保护，避免并发刷新。
   * 缓存有效时直接返回（< 1ms）；过期时同步获取新 token。
   */
  IF cachedToken != null AND System.currentTimeMillis() < expireTime THEN
    RETURN cachedToken
  END IF

  // 缓存过期或不存在，需要刷新
  SYNCHRONIZED(this):
    // 双重检查，避免并发重复刷新
    IF cachedToken != null AND System.currentTimeMillis() < expireTime THEN
      RETURN cachedToken
    END IF
    refreshAccessToken()
    RETURN cachedToken
  END SYNCHRONIZED


METHOD refreshAccessToken() -> void:
  /**
   * 从微信 API 获取新的 access_token 并更新缓存。
   * 仅在 synchronized 块内调用。
   */
  url = TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret

  TRY:
    response = restTemplate.getForObject(url, Map.class)

    IF response.containsKey("access_token") THEN
      cachedToken = response.get("access_token")
      expiresIn = response.get("expires_in")  // 通常 7200 秒
      // 提前 5 分钟刷新，避免边界过期
      expireTime = System.currentTimeMillis() + (expiresIn - ADVANCE_REFRESH_SECONDS) * 1000
      log.info("access_token 刷新成功，有效期 {}s", expiresIn)
    ELSE
      errcode = response.get("errcode")
      errmsg = response.get("errmsg")
      log.error("access_token 获取失败: errcode={}, errmsg={}", errcode, errmsg)
      THROW RuntimeException("微信 access_token 获取失败")
    END IF
  CATCH Exception e:
    log.error("access_token 获取异常: ", e)
    THROW RuntimeException("微信 access_token 获取失败")
```

### 2.4 配置来源

```
# application.yml 中已有微信配置（用于 jscode2session 登录）
# PST 复用这些配置
wx:
  appid: ${WX_APPID}
  secret: ${WX_SECRET}
```

**设计说明**：
- 参照现有 `UserService` 中微信 `jscode2session` 调用模式，复用 `wx.appid` 和 `wx.secret` 配置。
- 单实例部署（Docker Compose），内存缓存足够，无需 Redis。
- `volatile` 关键字保证多线程可见性。
- `synchronized` + 双重检查避免并发刷新导致的多次 API 调用。

---

## 3. WxacodeCacheManager 设计

### 3.1 职责

管理小程序码 Base64 数据的内存缓存。相同用户+活动的请求在 24 小时内返回缓存数据。

**需求来源**：REQ-PST-011（24h 缓存）、REQ-PST-023（LRU 500 条）

### 3.2 数据结构

```
Class: WxacodeCacheManager
  Annotations: @Component

  Inner Class: CacheEntry
    Fields:
      value      : String  -- Base64 编码的小程序码图片
      expireTime : long    -- 过期时间戳（毫秒）

  Fields:
    cache : LinkedHashMap<String, CacheEntry>  -- LRU 缓存容器

  Constants:
    MAX_CAPACITY = 500         -- 缓存容量上限（REQ-PST-023）
    TTL_MILLIS   = 86400000    -- 24 小时 TTL（REQ-PST-011）
```

### 3.3 LRU 实现方案

```
CONSTRUCTOR WxacodeCacheManager():
  // 使用 LinkedHashMap 的 accessOrder=true 模式实现 LRU
  // removeEldestEntry 在超过容量时自动淘汰最久未使用的条目
  cache = new LinkedHashMap<String, CacheEntry>(MAX_CAPACITY, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
      RETURN size() > MAX_CAPACITY
    }
  }
  // 外部访问使用 Collections.synchronizedMap 包装，保证线程安全
  cache = Collections.synchronizedMap(cache)
```

**设计说明**：
- 使用 JDK 内置的 `LinkedHashMap(accessOrder=true)` 实现 LRU，无需引入第三方库。
- `Collections.synchronizedMap` 提供基本线程安全。对于单实例低并发场景（画室小程序用户量有限）足够。
- 替代方案：`ConcurrentHashMap` + 自定义 LRU 逻辑，但复杂度更高，当前场景不需要。

### 3.4 核心方法

```
METHOD get(key: String) -> String | null:
  /**
   * 获取缓存的小程序码 Base64 数据。
   * key 格式："{userId}_{activityId}"
   * 返回 null 表示缓存未命中或已过期。
   */
  entry = cache.get(key)
  IF entry IS NULL THEN
    log.info("wxacode 缓存未命中: key={}", key)
    RETURN null
  END IF

  IF System.currentTimeMillis() > entry.expireTime THEN
    // 已过期，移除并返回 null
    cache.remove(key)
    log.info("wxacode 缓存已过期: key={}", key)
    RETURN null
  END IF

  log.info("wxacode 缓存命中: key={}", key)
  RETURN entry.value


METHOD put(key: String, value: String) -> void:
  /**
   * 存入小程序码 Base64 数据，TTL 24 小时。
   * 超过 MAX_CAPACITY 时 LinkedHashMap 自动淘汰最久未使用条目（BS-018）。
   */
  entry = new CacheEntry()
  entry.value = value
  entry.expireTime = System.currentTimeMillis() + TTL_MILLIS
  cache.put(key, entry)
  log.info("wxacode 缓存写入: key={}, cacheSize={}", key, cache.size())
```

### 3.5 缓存键设计

```
缓存键格式: "{userId}_{activityId}"
示例: "5_1", "12345_67890"

设计依据:
- userId 保证不同用户的小程序码 scene 参数不同（携带分享追踪信息）
- activityId 保证不同活动的小程序码跳转目标不同
- 下划线分隔，简单直观
```

---

## 4. 微信 API 调用数据映射

### 4.1 wxacode.getUnlimited 请求映射

```
微信 API 端点:
  POST https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token={ACCESS_TOKEN}

请求体 (JSON):
{
  "scene": "s={userId}&a={activityId}",  // 最大 32 字节
  "page": "pages/activity/activity",      // 小程序页面路径（不带 /）
  "width": 280,                           // 小程序码宽度（像素），默认 430
  "auto_color": false,                    // 不自动配色
  "line_color": {"r":99,"g":102,"b":241}  // 线条颜色 #6366F1（与主色一致）
}

字段映射:
  scene     <- "s=" + userId + "&a=" + activityId  (缩写格式，BS-013)
  page      <- 硬编码 "pages/activity/activity"     (REQ-PST-010)
  width     <- 280 (海报中小程序码显示 150x150px，280px 源尺寸保证清晰度)
  line_color <- RGB(99,102,241) = #6366F1           (设计风格一致)
```

### 4.2 wxacode.getUnlimited 响应映射

```
成功响应:
  Content-Type: image/jpeg 或 image/png
  Body: 图片二进制数据
  映射: byte[] -> Base64.encode -> String (存入缓存)

失败响应:
  Content-Type: application/json
  Body: {"errcode": 40001, "errmsg": "invalid credential"}
  映射: 解析 JSON，提取 errcode 和 errmsg，记录日志后抛出异常
```

### 4.3 常见微信 errcode

| errcode | 含义 | 处理方式 |
|---------|------|---------|
| 40001 | access_token 无效/过期 | 刷新 token 后重试 1 次 |
| 40013 | appid 无效 | 记录 ERROR 日志，不重试 |
| 45009 | API 调用频率超限 | 记录 WARN 日志，返回错误 |
| 41030 | page 路径不存在 | 记录 ERROR 日志，不重试 |

### 4.4 access_token 请求映射

```
微信 API 端点:
  GET https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={APPID}&secret={SECRET}

请求参数:
  grant_type <- "client_credential" (固定值)
  appid      <- application.yml wx.appid
  secret     <- application.yml wx.secret

成功响应:
  {"access_token": "ACCESS_TOKEN_STRING", "expires_in": 7200}
  映射: access_token -> cachedToken, expires_in -> 计算 expireTime

失败响应:
  {"errcode": 40013, "errmsg": "invalid appid"}
  映射: 记录 ERROR 日志，抛出 RuntimeException
```

---

## 5. 已有数据复用（无新增实体）

### 5.1 Activity 实体复用

PST 后端接口需要查询 Activity 以验证活动是否存在：

```
复用点:
  - ActivityMapper.selectById(activityId)  -- 验证活动存在
  - Activity.shareImage                     -- 前端海报背景图优先来源
  - Activity.coverImg                       -- 前端海报背景图备选来源
  - Activity.title                          -- 前端海报标题
  - Activity.startTime / endTime            -- 前端海报时间显示
```

### 5.2 StudioConfig 复用

前端绘制海报时需要画室名称：

```
复用点:
  - StudioConfig 表 config_key="studio_name" 的 config_value
  - 前端从 app.globalData.studioConfig 获取（已在 app.js onLaunch 中加载）
  - 无需后端新增接口，复用现有 GET /api/studio/config
```

### 5.3 User 上下文复用

后端需要 userId 构造 scene 参数：

```
复用点:
  - UserContext.getCurrentUserId()  -- 从 JWT 中提取
  - 参照 ActivityController.visitActivity() 中的使用方式
```

---

## 6. WxacodeData DTO 设计

### 6.1 响应 DTO

```
Class: WxacodeData
  Package: com.shuran.art.dto
  Annotations: @Data

  Fields:
    wxacodeBase64 : String  -- 小程序码图片 Base64 编码

  Constructor:
    WxacodeData(String wxacodeBase64)
```

**设计说明**：
- 作为 `Result<WxacodeData>` 的泛型参数使用。
- 不使用 `Map<String, Object>` 返回（虽然 ShareService.createShare 使用了 Map，但 DTO 更规范，JSON 字段名有编译期保证）。
- 放在 `com.shuran.art.dto` 包下，与 `Result.java`、`ShareRequest.java` 同目录。

---

## 7. 组件注册与依赖

### 7.1 新增组件清单

| 组件名 | 类型 | 包路径 | 说明 |
|--------|------|--------|------|
| WxAccessTokenManager | @Component | com.shuran.art.service | 微信 access_token 管理 |
| WxacodeCacheManager | @Component | com.shuran.art.service | 小程序码缓存管理 |
| WxacodeData | DTO | com.shuran.art.dto | wxacode 响应数据 |

### 7.2 ShareService 新增依赖

```
ShareService 现有依赖:
  - ShareRecordMapper
  - UserMapper
  - ActivityMapper

ShareService 新增依赖（PST 功能）:
  + WxAccessTokenManager
  + WxacodeCacheManager
  + RestTemplate (用于调用微信 wxacode API)

新增方法:
  + getWxacode(Long userId, Long activityId) -> WxacodeData
```

### 7.3 ShareController 新增端点

```
ShareController 现有端点:
  - POST /api/share/create
  - POST /api/share/confirm
  - GET  /api/share/status

ShareController 新增端点:
  + GET  /api/share/wxacode  (需 JWT 认证)
```

---

## 8. 需求追溯

| 需求编号 | 数据层设计覆盖 | 说明 |
|---------|--------------|------|
| REQ-PST-009 | WxAccessTokenManager, WxacodeCacheManager, 微信 API 映射 | 后端小程序码生成完整数据流 |
| REQ-PST-010 | 4.1 page 参数 | 跳转页面路径 |
| REQ-PST-011 | WxacodeCacheManager.get() TTL 检查 | 24h 缓存 |
| REQ-PST-022 | 缓存 get() < 200ms | 性能要求 |
| REQ-PST-023 | WxacodeCacheManager MAX_CAPACITY=500, LRU | 缓存策略 |
| BS-009 | 4.3 errcode 处理 | 微信 API 失败处理 |
| BS-013 | 4.1 scene 格式 | 缩写格式不超 32 字节 |
| BS-018 | 3.3 LRU LinkedHashMap | 缓存满时淘汰 |
| 决策 5 | WxAccessTokenManager 全部设计 | access_token 管理方案 |
