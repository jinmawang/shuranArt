# PST API 详细设计

> **L2 详设** -- 基于 L1 架构产出 `PST.openapi.yml`、`data-flow.md` 和现有 ShareService 模式细化。

## 1. 端点总览

| # | 方法 | 路径 | 认证 | 需求来源 | 代码参照 |
|---|------|------|------|---------|---------|
| EP-01 | GET | `/api/share/wxacode` | JWT | REQ-PST-009~011, REQ-PST-022~023 | ShareController 现有端点模式 |

PST 领域仅有一个后端端点。海报绘制、保存、分享、扫码着陆均为前端逻辑，不在后端 API 详设范围内，但会在测试设计中覆盖。

---

## 2. EP-01: GET /api/share/wxacode -- 生成活动小程序码

### 2.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败行为 |
|------|------|------|------|------|---------|---------|
| V-001 | activityId | query | Long | 是 | 正整数，对应已存在的活动 | 缺失时 Spring 400；活动不存在时 Result.error |
| V-002 | Authorization | header | String | 是 | 有效 JWT Bearer Token | AuthInterceptor 返回 401 |

```
VALIDATE(request):
  // JWT 认证由 AuthInterceptor 统一处理
  // 验证通过后 userId 存入 request.getAttribute("userId")
  userId = UserContext.getCurrentUserId()

  activityId = request.queryParam("activityId")
  IF activityId IS NULL THEN
    // Spring 框架自动返回 400 Bad Request（@RequestParam required=true 默认行为）
    ABORT
  END IF
```

### 2.2 业务逻辑

```
STEP-01: 提取用户 ID
  userId = UserContext.getCurrentUserId()
  // 参照 ActivityController.visitActivity() 获取 userId 的方式

STEP-02: 验证活动存在
  activity = activityMapper.selectById(activityId)
  IF activity IS NULL THEN
    RETURN Result.error("活动不存在")
  END IF
  // 注意：不检查活动状态。无论活动进行中/已结束/未开始均可生成小程序码
  // 依据：BS-015（已结束活动可分享）、BS-016（未开始活动可提前宣传）

STEP-03: 检查缓存
  cacheKey = userId + "_" + activityId
  cachedBase64 = wxacodeCacheManager.get(cacheKey)
  IF cachedBase64 IS NOT NULL THEN
    // 缓存命中，直接返回（响应时间 < 200ms，REQ-PST-022）
    RETURN Result.success(new WxacodeData(cachedBase64))
  END IF

STEP-04: 获取 access_token
  accessToken = wxAccessTokenManager.getAccessToken()
  // 详见 PST-data-detail.md WxAccessTokenManager 设计
  // 若获取失败，抛出 RuntimeException

STEP-05: 构造 scene 参数
  scene = "s=" + userId + "&a=" + activityId
  // 使用缩写格式确保不超过 32 字节（BS-013）
  // 示例：s=5&a=1（7 字节），s=12345&a=67890（17 字节）
  IF scene.getBytes("UTF-8").length > 32 THEN
    // 极端情况：userId 和 activityId 都是超大数字
    // 实际不太可能发生（Long 最大 19 位数字，s=1234567890123456789&a=1234567890123456789 = 43 字节）
    // 降级处理：仅携带 activityId
    scene = "a=" + activityId
  END IF

STEP-06: 调用微信 wxacode.getUnlimited API
  wxApiUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken
  requestBody = {
    "scene": scene,
    "page": "pages/activity/activity",
    "width": 280,          // 小程序码尺寸（像素）
    "auto_color": false,
    "line_color": {"r":99,"g":102,"b":241}  // 与主色 #6366F1 一致
  }

  TRY:
    response = httpClient.post(wxApiUrl, requestBody)

    // 微信 API 返回判断：
    // 成功：返回图片二进制数据（Content-Type: image/jpeg 或 image/png）
    // 失败：返回 JSON {errcode: xxx, errmsg: "..."}
    IF response.contentType STARTS WITH "image/" THEN
      imageBytes = response.body
    ELSE
      // 返回了 JSON 错误
      errorJson = parseJson(response.body)
      errcode = errorJson.get("errcode")
      errmsg = errorJson.get("errmsg")

      IF errcode == 40001 THEN
        // access_token 过期，刷新后重试一次
        GOTO STEP-07-RETRY
      END IF

      log.warn("微信 wxacode API 调用失败: errcode={}, errmsg={}", errcode, errmsg)
      THROW RuntimeException("小程序码生成失败，请稍后重试")
    END IF
  CATCH NetworkException:
    // 网络异常，重试一次
    GOTO STEP-07-RETRY

STEP-07: Base64 编码并缓存
  base64String = Base64.getEncoder().encodeToString(imageBytes)
  wxacodeCacheManager.put(cacheKey, base64String)
  // 缓存 24h TTL，LRU 淘汰，上限 500 条

STEP-08: 返回结果
  RETURN Result.success(new WxacodeData(base64String))

STEP-07-RETRY: 重试逻辑（最多 1 次）
  IF NOT alreadyRetried THEN
    alreadyRetried = true
    // 如果是 access_token 过期（errcode=40001），先刷新 token
    IF errcode == 40001 THEN
      wxAccessTokenManager.refreshAccessToken()
      accessToken = wxAccessTokenManager.getAccessToken()
    END IF
    GOTO STEP-06  // 重试调用微信 API
  ELSE
    log.error("微信 wxacode API 重试仍失败")
    THROW RuntimeException("小程序码生成失败，请稍后重试")
  END IF
```

**设计说明**：
- wxacode 接口挂载在 ShareController 下（决策 3：PST 属于分享上下文）。
- 实际实现为 ShareService 新增 `getWxacode(userId, activityId)` 方法。
- 重试策略：仅对网络异常和 access_token 过期进行重试，最多 1 次，无延迟。
- access_token 过期时的重试流程：刷新 token -> 用新 token 重调 wxacode API。
- Base64 编码不含 `data:image/png;base64,` 前缀，前端自行拼接。

### 2.3 出参构造

```
成功响应:
{
  "code": 0,
  "msg": "success",
  "data": {
    "wxacodeBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
  }
}

失败响应（活动不存在）:
{
  "code": -1,
  "msg": "活动不存在",
  "data": null
}

失败响应（微信 API 失败）:
{
  "code": -1,
  "msg": "小程序码生成失败，请稍后重试",
  "data": null
}
```

**出参类设计**：

```
Class: WxacodeData
  Fields:
    wxacodeBase64 : String  -- 小程序码图片的 Base64 编码字符串（PNG 格式）
```

**注意**：不使用 Map 返回，使用独立 DTO 类以保证 JSON 字段名固定。放在 `com.shuran.art.dto` 包下。

### 2.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 | 需求依据 |
|------|------|------|-----|---------|---------|
| 成功（首次生成） | 200 | 0 | "success" | 正常流程 | REQ-PST-009 |
| 成功（缓存命中） | 200 | 0 | "success" | 24h 内重复请求 | REQ-PST-011 |
| 活动不存在 | 200 | -1 | "活动不存在" | selectById 返回 null | AC-4.4 |
| 微信 API 失败 | 200 | -1 | "小程序码生成失败，请稍后重试" | 微信返回 errcode 或网络异常（重试后仍失败） | BS-009 |
| 未登录 | 401 | 401 | "未登录或登录已过期" | AuthInterceptor | AC-4.4 |
| 参数缺失 | 400 | -- | Spring 默认 | activityId 未传 | -- |

---

## 3. 前端逻辑设计（非后端 API，但需覆盖以保证完整性）

### 3.1 海报生成流程（前端 Canvas 2D）

对应需求：REQ-PST-001 ~ REQ-PST-004, REQ-PST-014

```
FUNCTION generatePoster(activity):

  STEP-01: 防重复点击
    IF this.data.isPosterGenerating THEN RETURN
    this.setData({ isPosterGenerating: true })
    wx.showLoading({ title: "海报生成中..." })

  STEP-02: 确保已登录
    IF NOT hasValidToken() THEN
      TRY:
        AWAIT silentLogin()  // 微信静默登录
      CATCH:
        wx.hideLoading()
        wx.showToast({ title: "登录失败，请重试", icon: "none" })
        this.setData({ isPosterGenerating: false })
        RETURN
    END IF

  STEP-03: 获取小程序码
    TRY:
      wxacodeResult = AWAIT wx.request({
        url: BASE_URL + "/api/share/wxacode",
        data: { activityId: activity.id },
        header: { "Authorization": "Bearer " + token }
      })
      IF wxacodeResult.data.code != 0 THEN
        THROW new Error(wxacodeResult.data.msg)
      END IF
      wxacodeBase64 = wxacodeResult.data.data.wxacodeBase64
    CATCH error:
      wx.hideLoading()
      wx.showToast({ title: "小程序码获取失败，请检查网络后重试", icon: "none", duration: 3000 })
      this.setData({ isPosterGenerating: false })
      RETURN

  STEP-04: 下载背景图
    bgImageUrl = activity.shareImage || activity.coverImg || "/images/default-activity.png"
    TRY:
      bgImagePath = AWAIT downloadImage(bgImageUrl, timeout=5000)
    CATCH:
      bgImagePath = "/images/default-activity.png"  // BS-008: 超时使用默认图

  STEP-05: 获取画室名称
    studioName = app.globalData.studioConfig?.studio_name || "舒然画室"  // BS-002

  STEP-06: 准备小程序码临时图片
    // 将 Base64 转为临时文件
    wxacodeArrayBuffer = wx.base64ToArrayBuffer(wxacodeBase64)
    wxacodeTempPath = AWAIT writeToTempFile(wxacodeArrayBuffer, "wxacode.png")

  STEP-07: Canvas 2D 绘制海报 (750x1334px)
    TRY:
      canvas = wx.createOffscreenCanvas({ type: "2d", width: 750, height: 1334 })
      ctx = canvas.getContext("2d")

      // 7a. 绘制背景图（上方 60%，即 0~800px）
      bgImage = canvas.createImage()
      AWAIT loadImage(bgImage, bgImagePath)
      ctx.drawImage(bgImage, 0, 0, 750, 800)  // aspectFill 裁剪

      // 7b. 绘制白色信息区（下方 40%，即 800~1334px）
      ctx.fillStyle = "#FFFFFF"
      ctx.fillRect(0, 800, 750, 534)

      // 7c. 绘制活动标题（BS-004: 超过 40 字截断）
      title = activity.title
      IF title.length > 40 THEN title = title.substring(0, 40) + "..."
      ctx.font = "bold 36px sans-serif"
      ctx.fillStyle = "#333333"
      ctx.textAlign = "center"
      ctx.fillText(title, 375, 870)

      // 7d. 绘制活动时间
      timeText = formatDate(activity.startTime) + " - " + formatDate(activity.endTime)
      ctx.font = "24px sans-serif"
      ctx.fillStyle = "#666666"
      ctx.fillText(timeText, 375, 920)

      // 7e. 绘制画室名称
      ctx.font = "28px sans-serif"
      ctx.fillStyle = "#6366F1"
      ctx.fillText(studioName, 375, 970)

      // 7f. 绘制小程序码（右下角，150x150px）
      wxacodeImage = canvas.createImage()
      AWAIT loadImage(wxacodeImage, wxacodeTempPath)
      // 白色圆角背景底
      ctx.fillStyle = "#FFFFFF"
      roundRect(ctx, 550, 1050, 170, 170, 10)
      ctx.fill()
      ctx.drawImage(wxacodeImage, 560, 1060, 150, 150)

      // 7g. 绘制"长按识别小程序码"提示文字
      ctx.font = "20px sans-serif"
      ctx.fillStyle = "#999999"
      ctx.textAlign = "center"
      ctx.fillText("长按识别小程序码", 635, 1240)

    CATCH canvasError:
      wx.hideLoading()
      wx.showToast({ title: "海报生成失败，请重试", icon: "none", duration: 3000 })
      this.setData({ isPosterGenerating: false })
      RETURN

  STEP-08: 导出临时图片
    posterTempPath = AWAIT canvasToTempFilePath(canvas, { fileType: "jpg", quality: 0.9 })

  STEP-09: 显示海报预览弹窗
    wx.hideLoading()
    this.setData({
      isPosterGenerating: false,
      showPosterModal: true,
      posterImagePath: posterTempPath
    })
```

### 3.2 海报保存（REQ-PST-005 ~ REQ-PST-007）

```
FUNCTION savePosterToAlbum():
  TRY:
    AWAIT wx.saveImageToPhotosAlbum({ filePath: this.data.posterImagePath })
    wx.showToast({ title: "保存成功", icon: "success" })
  CATCH error:
    IF error.errMsg CONTAINS "auth deny" THEN
      // 权限被拒绝（BS-010）
      wx.showModal({
        title: "提示",
        content: "请在设置中开启相册权限",
        confirmText: "去设置",
        cancelText: "取消",
        success: (res) => {
          IF res.confirm THEN wx.openSetting()
        }
      })
    ELSE
      wx.showToast({ title: "保存失败，请检查手机存储空间", icon: "none", duration: 3000 })
    END IF
```

### 3.3 海报分享（REQ-PST-008）

```
FUNCTION sharePosterToFriend():
  TRY:
    AWAIT wx.shareFileMessage({
      filePath: this.data.posterImagePath,
      fileName: "舒然画室活动海报.jpg"
    })
  CATCH error:
    IF error.errMsg CONTAINS "cancel" THEN
      // 用户取消，不提示
    ELSE
      wx.showToast({ title: "分享失败，请重试", icon: "none", duration: 3000 })
    END IF
```

### 3.4 扫码着陆（REQ-PST-012, REQ-PST-013）

```
// app.js onLaunch / onShow
FUNCTION handleSceneLaunch(options):
  IF options.query AND options.query.scene THEN
    scene = decodeURIComponent(options.query.scene)
    params = parseQueryString(scene)
    // 解析 "s=5&a=1" 格式
    shareFrom = params["s"]  // userId of sharer
    actId = params["a"]      // activityId

    IF actId THEN
      wx.navigateTo({
        url: "/pages/activity/activity?id=" + actId + (shareFrom ? "&shareFrom=" + shareFrom : "")
      })
    END IF
  END IF

// activity.js onLoad
FUNCTION onLoad(options):
  activityId = options.id
  shareFrom = options.shareFrom  // 可选，用于分享追踪

  IF activityId THEN
    loadActivity(activityId)
    IF shareFrom THEN
      // 传递给后续分享追踪逻辑（复用现有 SHR 模块）
      this.setData({ shareFrom: shareFrom })
    END IF
  ELSE
    // scene 参数无效，降级跳转首页
    wx.switchTab({ url: "/pages/index/index" })
  END IF
```

---

## 4. 需求追溯

| 需求编号 | 对应设计 | 覆盖说明 |
|---------|---------|---------|
| REQ-PST-001 | 3.1 STEP-07 | Canvas 绘制海报，包含封面图/标题/时间/画室名/小程序码 |
| REQ-PST-002 | 3.1 STEP-04 | shareImage 优先，其次 coverImg，最后默认图 |
| REQ-PST-003 | 3.1 STEP-03 | 调用后端接口获取小程序码 |
| REQ-PST-004 | 3.1 STEP-08~09 | canvasToTempFilePath 导出 + 弹窗显示 |
| REQ-PST-005 | 3.2 | saveImageToPhotosAlbum |
| REQ-PST-006 | 3.2 | 首次授权弹窗 |
| REQ-PST-007 | 3.2 | 拒绝权限后引导设置 |
| REQ-PST-008 | 3.3 | shareFileMessage |
| REQ-PST-009 | 2.2 STEP-02~08 | 后端 wxacode 生成完整流程 |
| REQ-PST-010 | 2.2 STEP-06 | page 参数 = "pages/activity/activity" |
| REQ-PST-011 | 2.2 STEP-03 | 24h 内缓存命中直接返回 |
| REQ-PST-012 | 3.4 | scene 参数解析 + 跳转活动详情页 |
| REQ-PST-013 | 3.4 | shareFrom 传递 |
| REQ-PST-014 | 3.1 STEP-01 | 防重复点击 + loading 态 |
| REQ-PST-020 | 3.1 STEP-07 | 750x1334px 分辨率 |
| REQ-PST-021 | 3.1 全流程 | 总耗时 < 5s |
| REQ-PST-022 | 2.2 STEP-03 | 首次 < 3s，缓存 < 200ms |
| REQ-PST-023 | 2.2 STEP-07 | LRU 500 条 24h TTL |
| BS-001 | 3.1 STEP-04 | 均为空时使用默认占位图 |
| BS-002 | 3.1 STEP-05 | 默认"舒然画室" |
| BS-004 | 3.1 STEP-07c | 截断 40 字 + "..." |
| BS-007 | 3.1 STEP-03 catch | Toast 提示 |
| BS-008 | 3.1 STEP-04 catch | 超时使用默认图 |
| BS-009 | 2.2 STEP-06 catch | 重试 + Result.error |
| BS-011 | 3.1 STEP-02 | 自动静默登录 |
| BS-012 | 3.1 STEP-01 | isPosterGenerating 防重复 |
| BS-013 | 2.2 STEP-05 | 缩写格式 scene |
| BS-014 | 3.1 弹窗关闭 | 点击外部区域关闭 |
| BS-015 | 2.2 STEP-02 注释 | 已结束活动可生成 |
| BS-016 | 2.2 STEP-02 注释 | 未开始活动可生成 |
| BS-017 | 3.4 | 活动不存在引导返回首页 |
| BS-018 | PST-data-detail.md | LRU 淘汰策略 |
