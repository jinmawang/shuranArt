# PST 测试设计

> **L2 详设** -- 基于 L0 验收标准（REQ-PST-001 ~ REQ-PST-023、BS-001 ~ BS-018）和 L2 API/数据层设计产出。

## 1. 测试点清单

| 编号 | 测试层级 | 测试目标 | 优先级 | 关联需求 |
|------|---------|---------|--------|---------|
| TP-PST-001 | 单元测试 | WxAccessTokenManager.getAccessToken 缓存命中 | P0 | 决策 5 |
| TP-PST-002 | 单元测试 | WxAccessTokenManager.getAccessToken 缓存过期刷新 | P0 | 决策 5 |
| TP-PST-003 | 单元测试 | WxAccessTokenManager.refreshAccessToken 微信 API 失败 | P1 | BS-009 |
| TP-PST-004 | 单元测试 | WxacodeCacheManager.get 缓存命中 | P0 | REQ-PST-011 |
| TP-PST-005 | 单元测试 | WxacodeCacheManager.get 缓存过期 | P0 | REQ-PST-011 |
| TP-PST-006 | 单元测试 | WxacodeCacheManager LRU 淘汰 | P1 | REQ-PST-023, BS-018 |
| TP-PST-007 | 单元测试 | ShareService.getWxacode 缓存命中路径 | P0 | REQ-PST-011 |
| TP-PST-008 | 单元测试 | ShareService.getWxacode 缓存未命中完整流程 | P0 | REQ-PST-009 |
| TP-PST-009 | 单元测试 | ShareService.getWxacode 活动不存在 | P0 | AC-4.4 |
| TP-PST-010 | 单元测试 | ShareService.getWxacode 微信 API 失败后重试 | P1 | BS-009 |
| TP-PST-011 | 单元测试 | ShareService.getWxacode scene 参数格式 | P1 | REQ-PST-009, BS-013 |
| TP-PST-012 | 单元测试 | ShareService.getWxacode access_token 过期重试 | P1 | error-strategy 3.1 |
| TP-PST-013 | 集成测试 | GET /api/share/wxacode 首次请求成功 | P0 | REQ-PST-009, REQ-PST-010 |
| TP-PST-014 | 集成测试 | GET /api/share/wxacode 缓存命中 | P0 | REQ-PST-011 |
| TP-PST-015 | 集成测试 | GET /api/share/wxacode 活动不存在 | P0 | AC-4.4 |
| TP-PST-016 | 集成测试 | GET /api/share/wxacode 未登录 401 | P0 | AC-4.4 |
| TP-PST-017 | 集成测试 | GET /api/share/wxacode 微信 API 失败 | P1 | BS-009 |
| TP-PST-018 | 集成测试 | GET /api/share/wxacode 缓存命中响应时间 | P2 | REQ-PST-022 |
| TP-PST-019 | 前端单元测试 | scene 参数解析（扫码着陆） | P0 | REQ-PST-012, REQ-PST-013 |
| TP-PST-020 | 前端单元测试 | 海报标题截断逻辑 | P1 | BS-004 |
| TP-PST-021 | 前端单元测试 | 背景图选择逻辑 | P1 | REQ-PST-002, BS-001 |

---

## 2. 单元测试骨架

### 2.1 TP-PST-001: WxAccessTokenManager 缓存命中

```
TEST: "getAccessToken - 缓存有效时直接返回 - 不调用微信 API"
  关联: 决策 5

  // Arrange
  tokenManager = new WxAccessTokenManager()
  // 模拟已缓存的有效 token
  setField(tokenManager, "cachedToken", "valid_token_123")
  setField(tokenManager, "expireTime", System.currentTimeMillis() + 3600000)  // 1 小时后过期

  // Act
  result = tokenManager.getAccessToken()

  // Assert
  assertThat(result).isEqualTo("valid_token_123")
  // 未调用微信 API
  verify(restTemplate, never()).getForObject(any(), any())
```

### 2.2 TP-PST-002: WxAccessTokenManager 缓存过期刷新

```
TEST: "getAccessToken - 缓存过期时调用微信 API 刷新"
  关联: 决策 5

  // Arrange
  tokenManager = new WxAccessTokenManager()
  setField(tokenManager, "cachedToken", "expired_token")
  setField(tokenManager, "expireTime", System.currentTimeMillis() - 1000)  // 已过期
  mock(restTemplate.getForObject(contains("cgi-bin/token"), Map.class))
    .returns(Map.of("access_token", "new_token_456", "expires_in", 7200))

  // Act
  result = tokenManager.getAccessToken()

  // Assert
  assertThat(result).isEqualTo("new_token_456")
  verify(restTemplate).getForObject(contains("cgi-bin/token"), eq(Map.class))
```

### 2.3 TP-PST-003: WxAccessTokenManager 微信 API 失败

```
TEST: "refreshAccessToken - 微信 API 返回错误 - 抛出 RuntimeException"
  关联: BS-009

  // Arrange
  tokenManager = new WxAccessTokenManager()
  setField(tokenManager, "cachedToken", null)
  setField(tokenManager, "expireTime", 0)
  mock(restTemplate.getForObject(contains("cgi-bin/token"), Map.class))
    .returns(Map.of("errcode", 40013, "errmsg", "invalid appid"))

  // Act & Assert
  assertThrows(RuntimeException.class, () -> tokenManager.getAccessToken())
    .hasMessageContaining("access_token 获取失败")
```

### 2.4 TP-PST-004: WxacodeCacheManager 缓存命中

```
TEST: "get - 缓存存在且未过期 - 返回缓存值"
  关联: REQ-PST-011

  // Arrange
  cacheManager = new WxacodeCacheManager()
  cacheManager.put("5_1", "base64data_abc")

  // Act
  result = cacheManager.get("5_1")

  // Assert
  assertThat(result).isEqualTo("base64data_abc")
```

### 2.5 TP-PST-005: WxacodeCacheManager 缓存过期

```
TEST: "get - 缓存存在但已过期 - 返回 null"
  关联: REQ-PST-011

  // Arrange
  cacheManager = new WxacodeCacheManager()
  cacheManager.put("5_1", "base64data_abc")
  // 手动将过期时间设为过去
  modifyCacheEntryExpireTime("5_1", System.currentTimeMillis() - 1000)

  // Act
  result = cacheManager.get("5_1")

  // Assert
  assertThat(result).isNull()
```

### 2.6 TP-PST-006: WxacodeCacheManager LRU 淘汰

```
TEST: "put - 超过 500 条上限时 LRU 淘汰最久未使用条目"
  关联: REQ-PST-023, BS-018

  // Arrange
  cacheManager = new WxacodeCacheManager()
  // 填充 500 条缓存
  FOR i = 1 TO 500:
    cacheManager.put("user_" + i, "data_" + i)
  END FOR
  // 访问第 1 条使其变为最近使用
  cacheManager.get("user_1")

  // Act
  // 添加第 501 条
  cacheManager.put("user_501", "data_501")

  // Assert
  assertThat(cacheManager.get("user_1")).isNotNull()    // 最近访问过，未被淘汰
  assertThat(cacheManager.get("user_2")).isNull()        // 最久未使用，被淘汰
  assertThat(cacheManager.get("user_501")).isNotNull()   // 新增条目存在
```

### 2.7 TP-PST-007: ShareService.getWxacode 缓存命中路径

```
TEST: "getWxacode - 缓存命中 - 不调用微信 API - 直接返回"
  关联: REQ-PST-011

  // Arrange
  mock(activityMapper.selectById(1L)).returns(mockActivity)
  mock(wxacodeCacheManager.get("5_1")).returns("cached_base64_data")

  // Act
  result = shareService.getWxacode(5L, 1L)

  // Assert
  assertThat(result.wxacodeBase64).isEqualTo("cached_base64_data")
  verify(wxAccessTokenManager, never()).getAccessToken()  // 未调用 token
  verify(restTemplate, never()).postForObject(any(), any(), any())  // 未调用微信 wxacode API
```

### 2.8 TP-PST-008: ShareService.getWxacode 完整流程

```
TEST: "getWxacode - 缓存未命中 - 调用微信 API 生成小程序码并缓存"
  关联: REQ-PST-009, REQ-PST-010

  // Arrange
  mock(activityMapper.selectById(1L)).returns(mockActivity)
  mock(wxacodeCacheManager.get("5_1")).returns(null)  // 缓存未命中
  mock(wxAccessTokenManager.getAccessToken()).returns("test_access_token")
  mockImageBytes = [0x89, 0x50, 0x4E, 0x47, ...]  // PNG 文件头
  mock(restTemplate.postForObject(
    contains("getwxacodeunlimit"),
    argThat(body -> body.get("scene") == "s=5&a=1" AND body.get("page") == "pages/activity/activity"),
    eq(byte[].class)
  )).returns(mockImageBytes)

  // Act
  result = shareService.getWxacode(5L, 1L)

  // Assert
  assertThat(result.wxacodeBase64).isNotBlank()
  assertThat(Base64.getDecoder().decode(result.wxacodeBase64)).isEqualTo(mockImageBytes)
  // 验证缓存写入
  verify(wxacodeCacheManager).put(eq("5_1"), anyString())
```

### 2.9 TP-PST-009: ShareService.getWxacode 活动不存在

```
TEST: "getWxacode - 活动不存在 - 抛出 RuntimeException"
  关联: AC-4.4（请求小程序码_活动不存在_返回错误）

  // Arrange
  mock(activityMapper.selectById(999L)).returns(null)

  // Act & Assert
  assertThrows(RuntimeException.class, () -> shareService.getWxacode(5L, 999L))
    .hasMessage("活动不存在")
```

### 2.10 TP-PST-011: scene 参数格式

```
TEST: "getWxacode - scene 参数使用缩写格式 s={userId}&a={actId}"
  关联: REQ-PST-009, BS-013

  // Arrange
  mock(activityMapper.selectById(1L)).returns(mockActivity)
  mock(wxacodeCacheManager.get("5_1")).returns(null)
  mock(wxAccessTokenManager.getAccessToken()).returns("token")
  mock(restTemplate.postForObject(any(), any(), any())).returns(mockImageBytes)

  // Act
  shareService.getWxacode(5L, 1L)

  // Assert
  verify(restTemplate).postForObject(
    any(),
    argThat(body -> {
      String scene = body.get("scene")
      RETURN scene == "s=5&a=1"
        AND scene.getBytes("UTF-8").length <= 32
    }),
    any()
  )
```

### 2.11 TP-PST-012: access_token 过期重试

```
TEST: "getWxacode - access_token 过期(errcode=40001) - 刷新 token 后重试成功"
  关联: error-strategy 3.1

  // Arrange
  mock(activityMapper.selectById(1L)).returns(mockActivity)
  mock(wxacodeCacheManager.get("5_1")).returns(null)
  mock(wxAccessTokenManager.getAccessToken()).returns("expired_token")

  // 第一次调用返回 errcode=40001（JSON 格式，非图片）
  firstCallResponse = '{"errcode":40001,"errmsg":"invalid credential"}'.getBytes()
  // 刷新 token 后第二次调用返回图片数据
  mock(wxAccessTokenManager.refreshAccessToken()).doesNothing()
  // 根据调用次数返回不同结果
  mock(restTemplate.postForObject(...))
    .thenReturn(firstCallResponse)  // 第一次：错误 JSON
    .thenReturn(mockImageBytes)      // 第二次：图片数据

  // Act
  result = shareService.getWxacode(5L, 1L)

  // Assert
  assertThat(result.wxacodeBase64).isNotBlank()
  verify(wxAccessTokenManager).refreshAccessToken()  // 触发了 token 刷新
```

---

## 3. 集成测试骨架

### 3.1 TP-PST-013: wxacode 首次请求成功

```
TEST: "GET /api/share/wxacode?activityId=1 - 首次请求返回 Base64 小程序码"
  关联: REQ-PST-009, REQ-PST-010

  // Arrange
  INSERT activity(id=1, title="暑期班", status=1)
  token = userJwtToken(userId=5)
  // Mock 微信 API（使用 WireMock 或类似工具）
  stubWxapiTokenEndpoint().returns({"access_token": "test_token", "expires_in": 7200})
  stubWxapiWxacodeEndpoint().returns(pngImageBytes)

  // Act
  response = GET /api/share/wxacode?activityId=1, headers: {Authorization: "Bearer " + token}

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data.wxacodeBase64).isNotBlank()
  // 验证 Base64 可解码为有效图片数据
  decodedBytes = Base64.decode(response.data.wxacodeBase64)
  assertThat(decodedBytes[0]).isEqualTo(0x89)  // PNG 文件头
```

### 3.2 TP-PST-014: wxacode 缓存命中

```
TEST: "GET /api/share/wxacode - 24h 内重复请求返回缓存数据"
  关联: REQ-PST-011

  // Arrange
  INSERT activity(id=1, title="暑期班", status=1)
  token = userJwtToken(userId=5)
  stubWxapiEndpoints()

  // Act - 第一次请求
  response1 = GET /api/share/wxacode?activityId=1, headers: {Authorization: "Bearer " + token}
  // Act - 第二次请求
  response2 = GET /api/share/wxacode?activityId=1, headers: {Authorization: "Bearer " + token}

  // Assert
  assertThat(response1.data.wxacodeBase64).isEqualTo(response2.data.wxacodeBase64)
  // 验证微信 wxacode API 只被调用了 1 次
  verifyWxacodeApiCalledTimes(1)
```

### 3.3 TP-PST-015: wxacode 活动不存在

```
TEST: "GET /api/share/wxacode?activityId=999 - 活动不存在返回错误"
  关联: AC-4.4

  // Arrange
  // 数据库中无 id=999 的活动
  token = userJwtToken(userId=5)

  // Act
  response = GET /api/share/wxacode?activityId=999, headers: {Authorization: "Bearer " + token}

  // Assert
  assertThat(response.code).isEqualTo(-1)
  assertThat(response.msg).isEqualTo("活动不存在")
```

### 3.4 TP-PST-016: wxacode 未登录

```
TEST: "GET /api/share/wxacode - 未携带 Token 返回 401"
  关联: AC-4.4

  // Arrange (no token)

  // Act
  response = GET /api/share/wxacode?activityId=1

  // Assert
  assertThat(response.httpStatus).isEqualTo(401)
```

### 3.5 TP-PST-018: 缓存命中响应时间

```
TEST: "GET /api/share/wxacode - 缓存命中时响应时间 < 200ms"
  关联: REQ-PST-022

  // Arrange
  INSERT activity(id=1, title="暑期班", status=1)
  token = userJwtToken(userId=5)
  stubWxapiEndpoints()
  // 先发一次请求填充缓存
  GET /api/share/wxacode?activityId=1, headers: {Authorization: "Bearer " + token}

  // Act
  startTime = System.currentTimeMillis()
  response = GET /api/share/wxacode?activityId=1, headers: {Authorization: "Bearer " + token}
  elapsed = System.currentTimeMillis() - startTime

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(elapsed).isLessThan(200)
```

---

## 4. 前端测试骨架

### 4.1 TP-PST-019: scene 参数解析

```
TEST: "parseScene - 标准格式 s=5&a=1 - 正确解析 shareFrom 和 actId"
  关联: REQ-PST-012, REQ-PST-013

  // Arrange
  scene = "s=5&a=1"

  // Act
  result = parseScene(scene)

  // Assert
  assertThat(result.shareFrom).isEqualTo("5")
  assertThat(result.actId).isEqualTo("1")

---

TEST: "parseScene - 仅有 actId (无 shareFrom) - actId 正确，shareFrom 为空"
  关联: REQ-PST-012

  // Arrange
  scene = "a=1"

  // Act
  result = parseScene(scene)

  // Assert
  assertThat(result.shareFrom).isNull()
  assertThat(result.actId).isEqualTo("1")

---

TEST: "parseScene - 空字符串 - 返回 null"
  关联: error-strategy 3.4

  // Arrange
  scene = ""

  // Act
  result = parseScene(scene)

  // Assert
  assertThat(result).isNull()

---

TEST: "parseScene - URL 编码的 scene - 正确解码并解析"
  关联: REQ-PST-012

  // Arrange
  scene = "s%3D5%26a%3D1"  // URL 编码的 "s=5&a=1"

  // Act
  decodedScene = decodeURIComponent(scene)
  result = parseScene(decodedScene)

  // Assert
  assertThat(result.actId).isEqualTo("1")
```

### 4.2 TP-PST-020: 海报标题截断

```
TEST: "truncateTitle - 标题 <= 40 字 - 不截断"
  关联: BS-004

  // Arrange
  title = "这是一个正常长度的标题"  // 9 个字

  // Act
  result = truncateTitle(title, 40)

  // Assert
  assertThat(result).isEqualTo("这是一个正常长度的标题")

---

TEST: "truncateTitle - 标题 > 40 字 - 截断并追加省略号"
  关联: BS-004

  // Arrange
  title = "这是一个超长标题" * 6  // 超过 40 字

  // Act
  result = truncateTitle(title, 40)

  // Assert
  assertThat(result.length).isEqualTo(43)  // 40 字 + "..."
  assertThat(result).endsWith("...")
```

### 4.3 TP-PST-021: 背景图选择逻辑

```
TEST: "selectBgImage - shareImage 存在 - 使用 shareImage"
  关联: REQ-PST-002

  // Arrange
  activity = { shareImage: "https://share.jpg", coverImg: "https://cover.jpg" }

  // Act
  result = selectBgImage(activity)

  // Assert
  assertThat(result).isEqualTo("https://share.jpg")

---

TEST: "selectBgImage - shareImage 为空 coverImg 存在 - 使用 coverImg"
  关联: REQ-PST-002

  // Arrange
  activity = { shareImage: null, coverImg: "https://cover.jpg" }

  // Act
  result = selectBgImage(activity)

  // Assert
  assertThat(result).isEqualTo("https://cover.jpg")

---

TEST: "selectBgImage - 均为空 - 使用默认占位图"
  关联: BS-001

  // Arrange
  activity = { shareImage: null, coverImg: null }

  // Act
  result = selectBgImage(activity)

  // Assert
  assertThat(result).isEqualTo("/images/default-activity.png")
```

---

## 5. 测试数据设计

### 5.1 基础测试数据

```
TestData: ACTIVITY_ACTIVE
  id: 1
  title: "暑期班报名优惠"
  coverImg: "/images/activity-summer.jpg"
  shareImage: "/images/share-default.jpg"
  startTime: "2026-06-01T00:00:00"
  endTime: "2026-08-31T23:59:59"
  status: 1

TestData: ACTIVITY_ENDED
  id: 2
  title: "春季活动"
  coverImg: "/images/spring.jpg"
  shareImage: null
  endTime: "2026-01-31T23:59:59"
  status: 1

TestData: MOCK_WXACODE_PNG
  // 最小有效 PNG 文件（用于 Mock 微信 API 返回）
  bytes: [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, ...]

TestData: USER_LOGGED_IN
  id: 5
  openid: "test_user_openid_5"
```

---

## 6. 优先级矩阵

| 优先级 | 测试点 | 数量 | 说明 |
|--------|--------|------|------|
| P0 | TP-PST-001, 002, 004, 005, 007, 008, 009, 013, 014, 015, 016, 019 | 12 | 核心功能：缓存/生成/认证/解析 |
| P1 | TP-PST-003, 006, 010, 011, 012, 017, 020, 021 | 8 | 边界/异常：LRU/重试/截断/图片选择 |
| P2 | TP-PST-018 | 1 | 性能：缓存响应时间 |

---

## 7. 需求追溯

| 需求编号 | 测试点编号 | 覆盖说明 |
|---------|-----------|---------|
| REQ-PST-002 | TP-PST-021 | 背景图选择逻辑 |
| REQ-PST-009 | TP-PST-008, 011, 013 | 小程序码生成 + scene 格式 |
| REQ-PST-010 | TP-PST-008, 013 | page 参数验证 |
| REQ-PST-011 | TP-PST-004, 005, 007, 014 | 24h 缓存命中 |
| REQ-PST-012 | TP-PST-019 | scene 参数解析 actId |
| REQ-PST-013 | TP-PST-019 | scene 参数解析 shareFrom |
| REQ-PST-022 | TP-PST-018 | 缓存命中响应时间 < 200ms |
| REQ-PST-023 | TP-PST-006 | LRU 500 条上限 |
| BS-001 | TP-PST-021 | 均为空时默认图 |
| BS-004 | TP-PST-020 | 标题截断 40 字 |
| BS-009 | TP-PST-003, 010, 017 | 微信 API 失败处理 |
| BS-013 | TP-PST-011 | scene 不超 32 字节 |
| BS-018 | TP-PST-006 | LRU 淘汰 |
| 决策 5 | TP-PST-001, 002, 003 | access_token 管理 |
