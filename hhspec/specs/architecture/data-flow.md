# 数据流 - CRS (课程管理) & PST (分享海报)

## 1. CRS 领域数据流

### 1.1 课程列表查询（用户端）

**触发**: 用户进入课程列表页，或在首页查看课程入口区域。

**需求来源**: EARS-CRS-001（课程列表）、EARS-CRS-002（分类筛选）、EARS-CRS-004（首页课程入口）

```mermaid
sequenceDiagram
    participant U as 用户(小程序)
    participant C as CourseController
    participant M as CourseMapper
    participant DB as MySQL

    U->>C: GET /api/course/list?category={可选}
    Note over C: 无需认证（excludePathPatterns）
    C->>M: selectList(status=1, orderBy sortOrder ASC)
    Note over M: 若 category 非空，追加 category 过滤条件
    M->>DB: SELECT * FROM course WHERE status=1 [AND category=?] ORDER BY sort_order ASC
    DB-->>M: List<Course>
    M-->>C: List<Course>
    C-->>U: Result.success(List<Course>)
    Note over U: 列表为空时前端显示"暂无课程信息"
```

**首页课程入口变体**:
- 首页加载时调用 `GET /api/course/list`（不传 category）
- 前端取返回列表的前 4 项展示为缩略卡片
- 点击卡片或"查看更多"跳转到课程列表页

### 1.2 课程详情查询（用户端）

**触发**: 用户在课程列表中点击某个课程卡片。

**需求来源**: EARS-CRS-003（课程详情）

```mermaid
sequenceDiagram
    participant U as 用户(小程序)
    participant C as CourseController
    participant M as CourseMapper
    participant DB as MySQL

    U->>C: GET /api/course/{id}
    Note over C: 无需认证（excludePathPatterns）
    C->>M: selectById(id)
    M->>DB: SELECT * FROM course WHERE id=?
    DB-->>M: Course | null
    M-->>C: Course | null
    C-->>U: Result.success(course)
    Note over U: data=null 时表示课程不存在，<br/>前端可按需处理（与 Activity 详情行为一致）
```

### 1.3 课程 CRUD（管理端）

**触发**: 管理员在管理后台对课程进行增删改查操作。

**需求来源**: EARS-CRS-005（管理后台 CRUD）

```mermaid
sequenceDiagram
    participant A as 管理员(小程序)
    participant Auth as AuthInterceptor
    participant Adm as AdminInterceptor
    participant C as AdminController
    participant S as AdminService
    participant M as CourseMapper
    participant DB as MySQL

    A->>Auth: 请求 /api/admin/course[s]<br/>Header: Authorization: Bearer <token>
    Auth->>Auth: 验证 JWT Token
    alt Token 无效
        Auth-->>A: HTTP 401 {code:401, msg:"未登录或登录已过期"}
    end
    Auth->>Adm: Token 有效，传递请求
    Adm->>Adm: 验证管理员白名单
    alt 非管理员
        Adm-->>A: HTTP 403 {code:403, msg:"无管理员权限"}
    end

    Note over A,DB: === 获取全部课程 ===
    A->>C: GET /api/admin/courses
    C->>S: getCourses()
    S->>M: selectList(orderBy sortOrder ASC)
    M->>DB: SELECT * FROM course ORDER BY sort_order ASC
    DB-->>A: Result.success(List<Course>)

    Note over A,DB: === 新增/编辑课程 ===
    A->>C: POST /api/admin/course {body}
    C->>S: saveCourse(course)
    alt id 为空（新增）
        S->>M: insert(course)
        M->>DB: INSERT INTO course (...)
    else id 有值（编辑）
        S->>M: updateById(course)
        M->>DB: UPDATE course SET ... WHERE id=?
    end
    DB-->>A: Result.success()

    Note over A,DB: === 上下架切换 ===
    A->>C: PUT /api/admin/course/{id}/status {status: 0|1}
    C->>S: updateCourseStatus(id, status)
    S->>M: update status
    M->>DB: UPDATE course SET status=? WHERE id=?
    DB-->>A: Result.success()

    Note over A,DB: === 删除课程 ===
    A->>C: DELETE /api/admin/course/{id}
    C->>S: deleteCourse(id)
    S->>M: deleteById(id)
    M->>DB: DELETE FROM course WHERE id=?
    DB-->>A: Result.success()
```

---

## 2. PST 领域数据流

### 2.1 海报生成流（主流程）

**触发**: 用户在活动详情页点击"生成海报"按钮。

**需求来源**: REQ-PST-001 ~ REQ-PST-004, REQ-PST-009 ~ REQ-PST-011, REQ-PST-014

```mermaid
sequenceDiagram
    participant U as 用户(小程序)
    participant SC as ShareController
    participant SS as ShareService
    participant TC as TokenCache<br/>(内存, 2h TTL)
    participant WC as WxacodeCache<br/>(ConcurrentHashMap<br/>LRU 500条, 24h TTL)
    participant WX as 微信 API
    participant DB as MySQL

    U->>U: 点击"生成海报"<br/>显示"海报生成中..."<br/>禁用按钮

    U->>SC: GET /api/share/wxacode?activityId={id}<br/>Header: Authorization: Bearer <token>
    Note over SC: AuthInterceptor 验证 JWT，提取 userId

    SC->>SS: getWxacode(userId, activityId)

    SS->>DB: SELECT * FROM activity WHERE id=?
    DB-->>SS: Activity | null
    alt 活动不存在
        SS-->>SC: 抛出异常 / 返回错误
        SC-->>U: Result.error("活动不存在")
        U->>U: 关闭加载态，显示 Toast
    end

    SS->>WC: get("{userId}_{activityId}")
    alt 缓存命中（24h 内）
        WC-->>SS: Base64 字符串
        SS-->>SC: wxacodeBase64
        SC-->>U: Result.success({wxacodeBase64: "..."})
    else 缓存未命中
        SS->>TC: getAccessToken()
        alt Token 缓存有效
            TC-->>SS: access_token
        else Token 过期或不存在
            TC->>WX: GET https://api.weixin.qq.com/cgi-bin/token<br/>?grant_type=client_credential<br/>&appid={appid}&secret={secret}
            WX-->>TC: {access_token, expires_in: 7200}
            TC->>TC: 缓存 access_token（2h TTL）
            TC-->>SS: access_token
        end

        SS->>WX: POST https://api.weixin.qq.com/wxa/getwxacodeunlimit<br/>?access_token={token}<br/>Body: {scene:"s={userId}&a={actId}",<br/>page:"pages/activity/activity"}
        alt 微信 API 成功
            WX-->>SS: 图片二进制数据 (PNG)
            SS->>SS: Base64 编码图片数据
            SS->>WC: put("{userId}_{activityId}", base64, 24h TTL)
            SS-->>SC: wxacodeBase64
            SC-->>U: Result.success({wxacodeBase64: "..."})
        else 微信 API 失败（重试1次）
            SS->>WX: 重试请求
            alt 重试成功
                WX-->>SS: 图片数据
                SS-->>SC: wxacodeBase64
                SC-->>U: Result.success({wxacodeBase64: "..."})
            else 重试仍失败
                SS-->>SC: 抛出异常
                SC-->>U: Result.error("小程序码生成失败，请稍后重试")
                U->>U: 关闭加载态，显示 Toast
            end
        end
    end

    Note over U: 获取小程序码成功后，前端继续
    U->>U: 下载活动封面图（shareImage 优先，其次 coverImg）
    Note over U: 若下载失败/超时(5s)，使用本地默认占位图
    U->>U: 获取画室名称（从 StudioConfig 或默认"舒然画室"）
    U->>U: Canvas 2D 绘制海报 (750x1334px)<br/>= 封面图 + 标题 + 时间 + 画室名 + 小程序码
    U->>U: canvasToTempFilePath 导出临时图片
    U->>U: 关闭加载态，显示海报预览弹窗
```

### 2.2 access_token 管理流

**触发**: 后端首次调用微信服务端 API，或缓存的 access_token 过期。

**需求来源**: 决策 5（微信 access_token 管理）

```mermaid
sequenceDiagram
    participant SS as ShareService
    participant TC as TokenCache<br/>(内存缓存)
    participant WX as 微信 API

    SS->>TC: getAccessToken()
    alt 缓存中有有效 token（未过期）
        TC-->>SS: access_token
        Note over SS: 直接使用，响应 < 1ms
    else 缓存为空或已过期
        TC->>WX: GET /cgi-bin/token<br/>?grant_type=client_credential<br/>&appid={wx.appid}<br/>&secret={wx.secret}
        alt 请求成功
            WX-->>TC: {access_token: "...", expires_in: 7200}
            TC->>TC: 存储 token<br/>设置过期时间 = now + 7200s - 300s (提前5分钟刷新)
            TC-->>SS: access_token
        else 请求失败
            TC-->>SS: 抛出异常
            Note over SS: 调用方（getWxacode）捕获后<br/>返回 Result.error
        end
    end
```

**实现要点**（参照 `application.yml` 中 `wx.appid` / `wx.secret` 配置）:
- access_token 存储在内存变量中（单实例部署，无需 Redis）
- 过期时间设为微信返回的 `expires_in` 减去 300 秒（提前刷新，避免边界过期）
- 线程安全：使用 `synchronized` 或 `AtomicReference` 保护 token 读写

### 2.3 海报保存与分享

**触发**: 用户在海报预览弹窗中点击"保存到相册"或"分享给好友"。

**需求来源**: REQ-PST-005 ~ REQ-PST-008

```mermaid
sequenceDiagram
    participant U as 用户(小程序)
    participant WX as 微信 API

    Note over U: 海报预览弹窗已显示

    alt 保存到相册
        U->>WX: wx.saveImageToPhotosAlbum({filePath})
        alt 已授权相册权限
            WX-->>U: 保存成功
            U->>U: 显示 Toast "保存成功"
        else 未授权（首次）
            WX-->>U: 弹出权限授权弹窗
            alt 用户同意授权
                WX-->>U: 保存成功
                U->>U: 显示 Toast "保存成功"
            else 用户拒绝授权
                WX-->>U: fail
                U->>U: 显示"请在设置中开启相册权限"<br/>+ "去设置"按钮
            end
        else 曾拒绝过权限
            WX-->>U: fail (auth denied)
            U->>U: 显示"请在设置中开启相册权限"<br/>+ "去设置"按钮
            U->>WX: [用户点击"去设置"] wx.openSetting()
        end
    else 分享给好友
        U->>WX: wx.shareFileMessage({filePath, fileName})
        alt 分享成功
            WX-->>U: success
        else 用户取消或失败
            WX-->>U: fail
            Note over U: 用户取消不提示；<br/>接口异常显示 Toast "分享失败，请重试"
        end
    end
```

### 2.4 扫码着陆流

**触发**: 用户扫描海报上的小程序码打开小程序。

**需求来源**: REQ-PST-012, REQ-PST-013

```mermaid
sequenceDiagram
    participant U as 用户
    participant App as app.js (onLaunch/onShow)
    participant AP as activity.js (onLoad)
    participant API as 后端 API

    U->>App: 扫码启动小程序<br/>options.query.scene = "s=5&a=1"
    App->>App: decodeURIComponent(scene)
    App->>App: 解析 scene 参数<br/>→ shareFrom=5, actId=1
    App->>AP: 跳转 /pages/activity/activity<br/>?id=1&shareFrom=5

    AP->>API: GET /api/activity/1
    API-->>AP: Result.success(activity)
    alt 活动存在
        AP->>AP: 渲染活动详情页
        Note over AP: shareFrom=5 传递给后续分享追踪逻辑<br/>（复用现有 share 模块流程）
    else 活动不存在(data=null)或已删除
        AP->>AP: 显示"活动不存在或已下架"<br/>引导用户返回首页
    end
```

---

## 3. 跨领域交互

### 3.1 CRS 与首页的集成

```
首页(index.js)
  └─ onLoad()
       ├─ GET /api/activity/list       → 活动列表（已有）
       ├─ GET /api/studio/config       → 画室配置（已有）
       └─ GET /api/course/list         → 课程列表（新增，取前4项展示）
```

CRS 领域与首页的集成是纯粹的数据读取关系，无写入交互，无事务耦合。

### 3.2 PST 与 SHR（分享上下文）的关系

PST 的小程序码接口 `GET /api/share/wxacode` 挂载在 ShareController 下，属于分享上下文（决策 3）。PST 复用了：
- **ShareController** 的路由前缀 `/api/share`
- **ShareService** 中新增 `getWxacode` 方法
- **Activity 实体** 的 `shareImage`、`coverImg` 字段
- **StudioConfig** 的 `studio_name` 配置

PST 不直接写入 `share_record` 表，小程序码的 scene 参数中携带 `shareFrom`，扫码后的分享追踪由现有 SHR 流程处理。
