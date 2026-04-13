---
task_id: "REQ-PST-001"
title: "分享海报生成功能"
type: "混合"
version: "1.0"
date: "2026-03-26"
author: "analyst-agent"
status: "draft"
priority: "high"
module: "PST"
related_changes:
  - "full-spec-gen"
tags:
  - "分享"
  - "海报"
  - "Canvas"
  - "小程序码"
---

# 分享海报生成功能

## 1. 概述

### 1.1 背景与动机

舒然画室小程序当前已具备基于微信原生转发的分享功能（`onShareAppMessage`），支持将活动链接分享给微信好友。但此方式仅限微信聊天场景，无法覆盖以下传播渠道：

- 用户保存图片到相册后发布到朋友圈
- 用户将图片通过其他社交平台（如小红书、抖音私信）传播
- 线下场景（打印海报、展示给他人扫码）

因此需要新增分享海报生成功能，使用前端 Canvas 技术在小程序端绘制包含活动信息和小程序码的海报图片，扩展分享传播渠道。

**证据来源**：
- 现有分享实现：`miniprogram/pages/activity/activity.js` 第 190-203 行 `onShareAppMessage` 方法
- 活动详情页当前仅有"分享活动"和"立即抽奖"两个底部操作按钮：`miniprogram/pages/activity/activity.wxml` 第 52-61 行
- 活动实体已有 `share_image`、`cover_img` 字段：`backend/src/main/java/com/shuran/art/entity/Activity.java` 第 14-15 行
- 画室名称已在 `studio_config` 表中配置为 `studio_name`：`mysql/init.sql` 第 162 行

### 1.2 目标用户

- **主要用户**：使用舒然画室小程序的家长和学员，希望将活动信息分享给更多人
- **间接用户**：通过海报图片扫码进入小程序的潜在学员和家长

### 1.3 范围

**In Scope（明确包含）**：
- 活动详情页新增"生成海报"按钮
- Canvas 绘制海报（包含活动封面图、活动标题、活动时间、画室名称、小程序码）
- 海报保存到手机相册
- 海报分享到微信好友/朋友圈
- 后端新增小程序码生成接口（调用微信 `wxacode.getUnlimited` API）
- 小程序码携带 `shareFrom` + `activityId` 参数用于分享追踪

**Out of Scope（明确排除）**：
- 海报模板自定义编辑（用户自选背景、字体等）
- 海报模板管理后台
- 多活动批量生成海报
- 海报分享后的裂变追踪闭环（扫码后自动确认分享记录）--该追踪依赖现有 share 模块的 `shareCode` 机制，扫码进入后走现有流程
- 后端图片合成（本方案为前端 Canvas 绘制，零服务器渲染成本）

## 2. 用户故事

- US-001: 作为活动页的访问用户，我希望在活动详情页点击"生成海报"按钮后看到一张包含活动信息和小程序码的海报图片，以便将活动以图片形式分享出去
- US-002: 作为已生成海报的用户，我希望将海报保存到手机相册，以便在朋友圈或其他平台发布
- US-003: 作为已生成海报的用户，我希望直接将海报分享给微信好友，以便无需先保存再手动发送
- US-004: 作为通过海报扫码进入小程序的新用户，我希望直接到达对应活动的详情页，以便了解活动内容并参与

## 3. 系统需求（EARS 格式）

### 3.1 功能需求

#### 3.1.1 海报生成（前端 Canvas 绘制）

- REQ-PST-001: 当用户在活动详情页点击"生成海报"按钮时，系统应使用 Canvas 2D API 绘制一张包含以下元素的海报图片：活动封面图、活动标题、活动时间（起止日期）、画室名称、小程序码。
- REQ-PST-002: 当活动存在 `shareImage` 字段值时，系统应优先使用 `shareImage` 作为海报背景图；当 `shareImage` 为空时，系统应使用 `coverImg` 作为海报背景图。
- REQ-PST-003: 当用户点击"生成海报"且系统尚未获取到小程序码时，系统应先调用后端接口 `GET /api/share/wxacode` 获取小程序码图片，再开始绘制海报。
- REQ-PST-004: 当海报绘制完成时，系统应将 Canvas 内容导出为临时图片文件路径（`tempFilePath`），并在海报预览弹窗中显示该图片。

#### 3.1.2 海报保存

- REQ-PST-005: 当用户在海报预览弹窗中点击"保存到相册"按钮时，系统应调用 `wx.saveImageToPhotosAlbum` 将海报图片保存到用户手机相册，并显示"保存成功"提示。
- REQ-PST-006: 当用户首次点击"保存到相册"且未授权相册权限时，系统应触发微信相册权限授权弹窗。
- REQ-PST-007: 当用户拒绝相册权限授权时，系统应显示引导提示"请在设置中开启相册权限"，并提供"去设置"按钮跳转到小程序权限设置页面。

#### 3.1.3 海报分享

- REQ-PST-008: 当用户在海报预览弹窗中点击"分享给好友"按钮时，系统应调用 `wx.shareFileMessage` 将海报图片文件发送给微信好友。

#### 3.1.4 后端小程序码生成

- REQ-PST-009: 当前端请求 `GET /api/share/wxacode?activityId={id}` 时，系统应调用微信 `wxacode.getUnlimited` API 生成携带 `scene=shareFrom={userId}&actId={activityId}` 参数的小程序码，并将小程序码图片以 Base64 编码字符串返回给前端。
- REQ-PST-010: 当后端调用微信 `wxacode.getUnlimited` API 时，系统应将小程序码的跳转页面（`page` 参数）设置为 `pages/activity/activity`。
- REQ-PST-011: 当相同用户和相同活动的小程序码在 24 小时内已生成过时，系统应返回缓存的小程序码数据，不重复调用微信 API。

#### 3.1.5 扫码着陆

- REQ-PST-012: 当用户通过扫描海报上的小程序码进入小程序时，系统应解析 `scene` 参数中的 `actId` 值，并跳转到对应活动的详情页（`/pages/activity/activity?id={actId}`）。
- REQ-PST-013: 当用户通过扫描海报上的小程序码进入小程序且 `scene` 参数包含 `shareFrom` 值时，系统应将 `shareFrom` 值传递给活动详情页，用于后续分享追踪。

#### 3.1.6 海报绘制进度

- REQ-PST-014: 当系统正在获取小程序码或绘制海报时，系统应显示加载态（带"海报生成中..."文案的 loading 指示器），阻止用户重复点击"生成海报"按钮。

### 3.2 非功能需求

- REQ-PST-020: 海报图片导出分辨率应为 750px x 1334px（iPhone 6/7/8 逻辑分辨率的 2 倍，适配主流手机屏幕清晰度）。
- REQ-PST-021: 从用户点击"生成海报"到海报预览弹窗显示的总耗时应不超过 5 秒（在 4G 网络环境下，小程序码接口首次请求 + Canvas 绘制 + 图片导出）。
- REQ-PST-022: 后端小程序码生成接口 `GET /api/share/wxacode` 的响应时间应不超过 3 秒（含微信 API 调用耗时；命中缓存时应不超过 200 毫秒）。
- REQ-PST-023: 小程序码缓存应使用内存缓存（如 `ConcurrentHashMap`），缓存容量上限为 500 条，采用 LRU 淘汰策略。

## 4. 验收标准（Gherkin 格式）

### 4.1 海报生成

```gherkin
Feature: 分享海报生成
  用户在活动详情页生成包含活动信息和小程序码的分享海报

  Scenario: 点击生成海报_活动有shareImage_显示含shareImage的海报预览
    # 关联需求: REQ-PST-001, REQ-PST-002, REQ-PST-004
    Given 用户已进入活动详情页
    And 该活动的 shareImage 值为 "https://example.com/share.jpg"
    And 该活动的 coverImg 值为 "https://example.com/cover.jpg"
    When 用户点击"生成海报"按钮
    Then 系统显示海报预览弹窗
    And 海报中的背景图为 "https://example.com/share.jpg"
    And 海报中包含活动标题文字
    And 海报中包含活动起止日期
    And 海报中包含画室名称
    And 海报中包含小程序码图片

  Scenario: 点击生成海报_活动无shareImage_显示含coverImg的海报预览
    # 关联需求: REQ-PST-002
    Given 用户已进入活动详情页
    And 该活动的 shareImage 值为空
    And 该活动的 coverImg 值为 "https://example.com/cover.jpg"
    When 用户点击"生成海报"按钮
    Then 系统显示海报预览弹窗
    And 海报中的背景图为 "https://example.com/cover.jpg"

  Scenario: 点击生成海报_显示加载态_生成完成后显示预览
    # 关联需求: REQ-PST-014
    Given 用户已进入活动详情页
    When 用户点击"生成海报"按钮
    Then 系统显示"海报生成中..."加载指示器
    And "生成海报"按钮变为不可点击状态
    When 海报生成完成
    Then 加载指示器消失
    And 系统显示海报预览弹窗

  Scenario: 生成海报过程中重复点击_不触发重复生成
    # 关联需求: REQ-PST-014
    Given 用户已进入活动详情页
    And 海报正在生成中（加载态）
    When 用户再次点击"生成海报"按钮
    Then 系统不触发新的海报生成流程
```

### 4.2 海报保存

```gherkin
Feature: 海报保存到相册
  用户将生成的海报图片保存到手机相册

  Scenario: 点击保存到相册_已授权_保存成功
    # 关联需求: REQ-PST-005
    Given 海报预览弹窗已显示
    And 用户已授权相册写入权限
    When 用户点击"保存到相册"按钮
    Then 海报图片被保存到手机相册
    And 系统显示"保存成功"提示

  Scenario: 点击保存到相册_未授权_弹出权限授权
    # 关联需求: REQ-PST-006
    Given 海报预览弹窗已显示
    And 用户未授权相册写入权限
    When 用户点击"保存到相册"按钮
    Then 系统弹出微信相册权限授权弹窗

  Scenario: 拒绝相册权限_显示引导设置提示
    # 关联需求: REQ-PST-007
    Given 海报预览弹窗已显示
    And 用户已拒绝过相册权限授权
    When 用户点击"保存到相册"按钮
    Then 系统显示提示"请在设置中开启相册权限"
    And 系统显示"去设置"按钮

  Scenario: 点击去设置按钮_跳转权限设置页
    # 关联需求: REQ-PST-007
    Given 系统已显示"请在设置中开启相册权限"提示
    When 用户点击"去设置"按钮
    Then 系统跳转到小程序权限设置页面
```

### 4.3 海报分享

```gherkin
Feature: 海报分享给好友
  用户将生成的海报图片直接分享给微信好友

  Scenario: 点击分享给好友_调起微信分享
    # 关联需求: REQ-PST-008
    Given 海报预览弹窗已显示
    When 用户点击"分享给好友"按钮
    Then 系统调起微信好友选择界面
    And 分享内容为海报图片文件
```

### 4.4 后端小程序码生成

```gherkin
Feature: 小程序码生成接口
  后端调用微信API生成携带追踪参数的小程序码

  Scenario: 请求小程序码_首次请求_返回新生成的Base64编码图片
    # 关联需求: REQ-PST-009, REQ-PST-010
    Given 用户已登录（持有有效 JWT Token）
    And 活动 ID 为 1 的活动存在且状态为进行中
    And 该用户未在 24 小时内请求过活动 1 的小程序码
    When 前端请求 "GET /api/share/wxacode?activityId=1"
    Then 系统返回 HTTP 200
    And 响应数据包含字段 "wxacodeBase64"，值为非空 Base64 编码字符串
    And 微信 API 被调用时的 scene 参数包含 "shareFrom={userId}&actId=1"
    And 微信 API 被调用时的 page 参数为 "pages/activity/activity"

  Scenario: 请求小程序码_24小时内重复请求_返回缓存数据
    # 关联需求: REQ-PST-011
    Given 用户已登录
    And 该用户在 24 小时内已请求过活动 1 的小程序码
    When 前端请求 "GET /api/share/wxacode?activityId=1"
    Then 系统返回 HTTP 200
    And 响应数据包含字段 "wxacodeBase64"，值与首次请求一致
    And 微信 wxacode.getUnlimited API 未被调用

  Scenario: 请求小程序码_活动不存在_返回错误
    # 关联需求: REQ-PST-009
    Given 用户已登录
    And 活动 ID 为 999 的活动不存在
    When 前端请求 "GET /api/share/wxacode?activityId=999"
    Then 系统返回错误提示"活动不存在"

  Scenario: 请求小程序码_未登录_返回401
    # 关联需求: REQ-PST-009
    Given 用户未登录（无 JWT Token）
    When 前端请求 "GET /api/share/wxacode?activityId=1"
    Then 系统返回 HTTP 401
```

### 4.5 扫码着陆

```gherkin
Feature: 海报扫码着陆
  用户扫描海报上的小程序码进入活动详情页

  Scenario: 扫码进入_scene包含actId_跳转活动详情页
    # 关联需求: REQ-PST-012
    Given 用户扫描了海报上的小程序码
    And 小程序码的 scene 参数为 "shareFrom=5&actId=1"
    When 小程序启动
    Then 用户被导航到活动详情页 "/pages/activity/activity"
    And 页面接收到参数 id 值为 "1"

  Scenario: 扫码进入_scene包含shareFrom_传递分享追踪参数
    # 关联需求: REQ-PST-013
    Given 用户扫描了海报上的小程序码
    And 小程序码的 scene 参数为 "shareFrom=5&actId=1"
    When 小程序启动并到达活动详情页
    Then 页面接收到参数 shareFrom 值为 "5"
```

## 5. 边界场景清单

| 编号 | 类别 | 场景描述 | 关联需求 | 预期行为 |
|------|------|----------|----------|----------|
| BS-001 | 空值 | 活动的 shareImage 和 coverImg 均为空 | REQ-PST-002 | 系统应使用本地默认占位图（`/images/default-activity.png`）作为海报背景图 |
| BS-002 | 空值 | 画室名称（studio_name）未配置 | REQ-PST-001 | 系统应使用默认值"舒然画室"作为画室名称显示在海报中 |
| BS-003 | 空值 | 活动标题为空字符串 | REQ-PST-001 | 系统应在海报标题区域不显示文字，不导致绘制异常或崩溃 |
| BS-004 | 极值 | 活动标题超过 40 个汉字（80 字节） | REQ-PST-001 | 系统应截断标题并追加省略号"..."，保证海报布局不溢出 |
| BS-005 | 极值 | 活动封面图尺寸极小（如 10x10 像素） | REQ-PST-002 | 系统应将图片拉伸/裁剪填充到海报背景区域，不留空白 |
| BS-006 | 极值 | 活动封面图尺寸极大（超过 5MB） | REQ-PST-002 | 系统应在图片下载超时（5 秒）后使用默认占位图 |
| BS-007 | 网络 | 获取小程序码时网络请求失败 | REQ-PST-003 | 系统应显示错误提示"小程序码获取失败，请检查网络后重试"，关闭加载态，允许用户重新点击 |
| BS-008 | 网络 | 下载活动封面图时网络请求超时 | REQ-PST-001 | 系统应在超时（5 秒）后使用本地默认占位图继续绘制海报 |
| BS-009 | 网络 | 后端调用微信 wxacode.getUnlimited API 时微信服务不可用 | REQ-PST-009 | 系统应返回 HTTP 500 错误，响应体包含错误信息"小程序码生成失败，请稍后重试" |
| BS-010 | 权限 | 用户在 iOS 上拒绝相册权限后再次点击保存 | REQ-PST-007 | 系统应显示"请在设置中开启相册权限"提示和"去设置"按钮 |
| BS-011 | 权限 | 用户未登录时点击生成海报 | REQ-PST-003 | 系统应先触发微信登录流程，登录成功后自动继续海报生成流程 |
| BS-012 | 并发 | 用户快速连续点击"生成海报"按钮 | REQ-PST-014 | 系统应在第一次点击后禁用按钮，仅执行一次海报生成流程 |
| BS-013 | 数据完整性 | 小程序码的 scene 参数超过 32 字节限制 | REQ-PST-009 | 系统应使用缩写格式（`s={userId}&a={actId}`）确保 scene 参数不超过 32 字节 |
| BS-014 | 状态流转 | 海报预览弹窗显示中用户点击弹窗外部区域 | REQ-PST-004 | 系统应关闭海报预览弹窗，返回活动详情页正常状态 |
| BS-015 | 状态流转 | 活动已结束（ended=true）时用户点击"生成海报" | REQ-PST-001 | 系统应允许生成海报（已结束的活动仍可分享，但海报中应显示活动已结束状态） |
| BS-016 | 状态流转 | 活动未开始时用户点击"生成海报" | REQ-PST-001 | 系统应允许生成海报（未开始的活动可提前宣传） |
| BS-017 | 数据完整性 | scene 参数中 actId 对应的活动已被删除 | REQ-PST-012 | 系统应显示"活动不存在或已下架"提示，引导用户返回首页 |
| BS-018 | 极值 | 缓存容量达到 500 条上限后有新请求 | REQ-PST-023 | 系统应按 LRU 策略淘汰最久未使用的缓存条目，为新条目腾出空间 |

## 6. UI 交互流程

### 6.1 成功路径

1. 用户在活动详情页（`/pages/activity/activity`）浏览活动内容
2. 用户点击底部操作栏的"生成海报"按钮
3. 系统显示"海报生成中..."加载指示器，"生成海报"按钮变为禁用状态
4. 系统在后台完成：(a) 调用 `GET /api/share/wxacode` 获取小程序码 (b) 下载活动封面图 (c) 获取画室名称 (d) Canvas 绘制海报 (e) 导出海报为临时图片
5. 加载指示器消失，系统弹出海报预览弹窗（覆盖层 + 海报图片 + 操作按钮）
6. 海报预览弹窗显示：海报图片（居中显示）、"保存到相册"按钮、"分享给好友"按钮、关闭按钮（右上角X）
7a. 用户点击"保存到相册" -> 系统保存图片 -> 显示"保存成功"Toast -> 弹窗保持显示
7b. 用户点击"分享给好友" -> 系统调起微信好友选择界面 -> 用户选择好友发送 -> 返回弹窗
7c. 用户点击关闭按钮或弹窗外部区域 -> 弹窗关闭 -> 返回活动详情页

### 6.2 错误路径

| 错误点 | 触发条件 | 系统反馈 | 恢复操作 |
|--------|----------|----------|----------|
| 小程序码获取失败 | 网络异常或后端接口错误 | 关闭加载态，显示 Toast "小程序码获取失败，请检查网络后重试"（居中显示，3 秒自动消失） | 用户可重新点击"生成海报"按钮 |
| 活动封面图下载失败 | 图片 URL 无效或网络超时 | 使用本地默认占位图继续生成海报，不中断流程 | 无需用户操作 |
| Canvas 绘制异常 | 设备内存不足或 Canvas API 调用失败 | 关闭加载态，显示 Toast "海报生成失败，请重试"（居中显示，3 秒自动消失） | 用户可重新点击"生成海报"按钮 |
| 保存相册失败（权限拒绝） | 用户曾拒绝相册权限 | 显示模态弹窗"请在设置中开启相册权限"，包含"去设置"和"取消"按钮 | 用户点击"去设置"跳转权限设置页；点击"取消"返回海报预览弹窗 |
| 保存相册失败（其他原因） | 存储空间不足等系统原因 | 显示 Toast "保存失败，请检查手机存储空间"（居中显示，3 秒自动消失） | 用户清理存储后可重新点击"保存到相册" |
| 分享失败 | 用户取消分享或微信接口异常 | 不显示错误提示（用户主动取消不算错误）；接口异常时显示 Toast "分享失败，请重试" | 用户可重新点击"分享给好友" |
| 用户未登录 | 点击"生成海报"时无有效 token | 系统自动触发微信登录流程（静默登录） | 登录成功后自动继续海报生成；登录失败显示 Toast "登录失败，请重试" |

### 6.3 状态矩阵

| 状态 | 可见元素 | 可执行操作 | 禁用元素 |
|------|----------|------------|----------|
| 活动详情页-默认态 | 活动封面图、标题、时间、详情、规则、联系老师、底部操作栏（"分享活动""生成海报""立即抽奖"） | 点击"生成海报"、点击"分享活动"、点击"立即抽奖"、滚动浏览 | 无 |
| 活动详情页-海报生成中 | 同上 + "海报生成中..."加载指示器（覆盖在按钮上或页面中央） | 滚动浏览、点击"立即抽奖" | "生成海报"按钮（禁用态：灰色不可点击） |
| 海报预览弹窗-显示态 | 半透明黑色遮罩层、海报图片（居中）、"保存到相册"按钮、"分享给好友"按钮、关闭按钮（右上角X） | 点击"保存到相册"、点击"分享给好友"、点击关闭按钮、点击遮罩层关闭 | 底部操作栏（被遮罩覆盖） |
| 海报预览弹窗-保存中 | 同"海报预览弹窗-显示态" + "保存中..."加载��� | 点击"分享给好友"、点击关闭按钮 | "保存到相册"按钮（禁用态） |
| 权限引导弹窗-显示态 | 模态弹窗："请在设置中开启相册权限"文案、"去设置"按钮、"取消"按钮 | 点击"去设置"、点击"取消" | 海报预览弹窗内的按钮（被模态弹窗覆盖） |

## 7. 验收策略

### 7.1 验收方法

本需求为混合需求（功能需求 + UI 需求 + 后端接口），按子需求类型分别验收：

#### 7.1.1 功能子需求（海报生成、保存、分享、小程序码接口、扫码着陆）

```yaml
acceptance_strategy:
  type: functional
  method: gherkin_execution
  verification_steps:
    - "L2 产出测试骨架 -> L3 填充实现 -> L4 自动执行"
    - "所有 Gherkin 场景 PASS -> 通过"
    - "边界场景覆盖率 >= Challenger 审查通过的清单"
```

#### 7.1.2 UI 子需求（海报预览弹窗、按钮交互）

```yaml
acceptance_strategy:
  type: ui_without_prototype
  method: interaction_verification
  visual_criteria:
    - "海报预览弹窗居中显示，遮罩层覆盖全屏"
    - "海报图片宽度占屏幕宽度的 85%，垂直居中"
    - "操作按钮排列在海报下方，左右分布"
    - "配色与活动详情页一致（主色 #6366F1 蓝紫色系）"
  interaction_criteria:
    - "交互流程与 6.1/6.2 描述一致"
    - "所有状态转换符合 6.3 状态矩阵定义"
    - "加载态/禁用态视觉反馈明确可辨"
  verification_steps:
    - "交互走查：按 Gherkin 场景逐步操作"
    - "状态矩阵验证：逐行核对可见元素和可执行操作"
```

#### 7.1.3 后端接口子需求（小程序码生成 + 缓存）

```yaml
acceptance_strategy:
  type: technical
  method: metric_comparison
  baseline:
    metric: "wxacode 接口响应时间"
    current_value: "不适用（新接口）"
    measurement_tool: "后端单元测试 + 集成测试"
  target:
    metric: "wxacode 接口响应时间"
    target_value: "首次请求 < 3 秒，命中缓存 < 200 毫秒"
    condition: "单用户请求"
  verification_steps:
    - "单元测试验证缓存命中/未命中逻辑"
    - "集成测试验证微信 API 调用正确性（可 mock）"
    - "性能测试验证缓存响应时间 < 200 毫秒"
```

### 7.2 覆盖度清单

```yaml
coverage_checklist:
  requirement_coverage:
    - requirement: "REQ-PST-001"
      gherkin_scenarios:
        - "点击生成海报_活动有shareImage_显示含shareImage的海报预览"
        - "点击生成海报_活动无shareImage_显示含coverImg的海报预览"
      boundary_scenarios:
        - "BS-001"
        - "BS-002"
        - "BS-003"
        - "BS-004"
    - requirement: "REQ-PST-002"
      gherkin_scenarios:
        - "点击生成海报_活动有shareImage_显示含shareImage的海报预览"
        - "点击生成海报_活动无shareImage_显示含coverImg的海报预览"
      boundary_scenarios:
        - "BS-001"
        - "BS-005"
        - "BS-006"
    - requirement: "REQ-PST-003"
      gherkin_scenarios:
        - "请求小程序码_首次请求_返回新生成的Base64编码图片"
      boundary_scenarios:
        - "BS-007"
        - "BS-011"
    - requirement: "REQ-PST-004"
      gherkin_scenarios:
        - "点击生成海报_显示加载态_生成完成后显示预览"
      boundary_scenarios:
        - "BS-014"
    - requirement: "REQ-PST-005"
      gherkin_scenarios:
        - "点击保存到相册_已授权_保存成功"
      boundary_scenarios: []
    - requirement: "REQ-PST-006"
      gherkin_scenarios:
        - "点击保存到相册_未授权_弹出权限授权"
      boundary_scenarios: []
    - requirement: "REQ-PST-007"
      gherkin_scenarios:
        - "拒绝相册权限_显示引导设置提示"
        - "点击去设置按钮_跳转权限设置页"
      boundary_scenarios:
        - "BS-010"
    - requirement: "REQ-PST-008"
      gherkin_scenarios:
        - "点击分享给好友_调起微信分享"
      boundary_scenarios: []
    - requirement: "REQ-PST-009"
      gherkin_scenarios:
        - "请求小程序码_首次请求_返回新生成的Base64编码图片"
        - "请求小程序码_活动不存在_返回错误"
        - "请求小程序码_未登录_返回401"
      boundary_scenarios:
        - "BS-009"
        - "BS-013"
    - requirement: "REQ-PST-010"
      gherkin_scenarios:
        - "请求小程序码_首次请求_返回新生成的Base64编码图片"
      boundary_scenarios: []
    - requirement: "REQ-PST-011"
      gherkin_scenarios:
        - "请求小程序码_24小时内重复请求_返回缓存数据"
      boundary_scenarios:
        - "BS-018"
    - requirement: "REQ-PST-012"
      gherkin_scenarios:
        - "扫码进入_scene包含actId_跳转活动详情页"
      boundary_scenarios:
        - "BS-017"
    - requirement: "REQ-PST-013"
      gherkin_scenarios:
        - "扫码进入_scene包含shareFrom_传递分享追踪参数"
      boundary_scenarios: []
    - requirement: "REQ-PST-014"
      gherkin_scenarios:
        - "点击生成海报_显示加载态_生成完成后显示预览"
        - "生成海报过程中重复点击_不触发重复生成"
      boundary_scenarios:
        - "BS-012"

  path_coverage:
    success_paths:
      - "生成海报 -> 预览 -> 保存到相册"
      - "生成海报 -> 预览 -> 分享给好友"
      - "生成海报 -> 预览 -> 关闭弹窗"
      - "扫码进入 -> 解析scene -> 跳转活动详情页"
    error_paths:
      - "生成海报 -> 小程序码获取失败 -> 显示错误提示 -> 重试"
      - "生成海报 -> 封面图下载失败 -> 使用默认图继续"
      - "生成海报 -> Canvas绘制异常 -> 显示错误提示 -> 重试"
      - "保存到相册 -> 权限被拒绝 -> 显示引导 -> 去设置"
      - "保存到相册 -> 存储空间不足 -> 显示错误提示"
      - "未登录 -> 自动登录 -> 登录成功继续 / 登录失败提示"
      - "扫码进入 -> 活动已删除 -> 显示提示引导返回首页"

  field_coverage:
    entity_poster_canvas:
      - field: "背景图（活动封面）"
        source: "Activity.shareImage（优先）或 Activity.coverImg"
        validation: "图片 URL 非空时加载网络图片；为空时使用 /images/default-activity.png"
        display_format: "充满海报顶部区域，aspectFill 裁剪模式"
      - field: "活动标题"
        source: "Activity.title"
        validation: "字符串类型；超过 40 个汉字时截断并追加省略号"
        display_format: "白色文字，字号 36px，加粗，居中对齐，最多显示 2 行"
      - field: "活动时间"
        source: "Activity.startTime + Activity.endTime"
        validation: "日期时间类型，格式化为 YYYY-MM-DD"
        display_format: "白色文字，字号 24px，格式：{startDate} - {endDate}"
      - field: "画室名称"
        source: "StudioConfig['studio_name']"
        validation: "字符串类型；为空时使用默认值'舒然画室'"
        display_format: "白色文字，字号 28px，居中对齐"
      - field: "小程序码"
        source: "GET /api/share/wxacode 返回的 Base64 图片"
        validation: "Base64 编码的 PNG/JPEG 图片数据"
        display_format: "海报右下角，宽高 150px x 150px，白色圆角背景底"
    entity_wxacode_request:
      - field: "activityId"
        source: "URL Query 参数"
        validation: "Long 类型，必填，必须对应已存在的活动"
        display_format: "不适用"
    entity_wxacode_response:
      - field: "wxacodeBase64"
        source: "微信 wxacode.getUnlimited API 返回的图片数据"
        validation: "非空 Base64 编码字符串"
        display_format: "不适用"
    entity_scene_parameter:
      - field: "scene"
        source: "小程序码二维码数据"
        validation: "字符串类型，最大 32 字节，格式：s={userId}&a={actId}"
        display_format: "不适用（URL 参数编码）"
      - field: "shareFrom (s)"
        source: "scene 参数中解析"
        validation: "数字类型，对应 user.id"
        display_format: "不适用"
      - field: "actId (a)"
        source: "scene 参数中解析"
        validation: "数字类型，对应 activity.id"
        display_format: "不适用"
    entity_wxacode_cache:
      - field: "缓存键"
        source: "userId + activityId 组合"
        validation: "字符串拼接，格式：{userId}_{activityId}"
        display_format: "不适用"
      - field: "缓存值"
        source: "Base64 编码的小程序码图片"
        validation: "非空字符串"
        display_format: "不适用"
      - field: "缓存有效期"
        source: "系统配置"
        validation: "24 小时（86400 秒）"
        display_format: "不适用"
      - field: "缓存容量上限"
        source: "系统配置"
        validation: "500 条"
        display_format: "不适用"

  screen_coverage:
    screen_activity_detail_page:
      display_fields:
        - "活动封面图（activity.coverImg）"
        - "活动标题（activity.title）"
        - "活动状态标签（进行中/即将开始）"
        - "活动时间（startDate - endDate）"
        - "活动详情（activity.description）"
        - "活动规则（硬编码4条规则文案）"
        - "联系老师区域（studioWechatId / studioQrcode）"
      input_fields: []
      action_elements:
        - element: "分享活动按钮"
          action: "触发微信原生分享（open-type=share）"
          condition: "活动已开始时可点击；未开始时灰色禁用"
        - element: "生成海报按钮（新增）"
          action: "触发海报生成流程"
          condition: "活动存在时可点击；海报生成中时灰色禁用"
        - element: "立即抽奖按钮"
          action: "跳转到抽奖页面（switchTab /pages/lottery/lottery）"
          condition: "始终可点击"
    screen_poster_preview_modal:
      display_fields:
        - "半透明黑色遮罩层（rgba(0,0,0,0.7)）"
        - "海报图片（Canvas 导出的临时图片）"
      input_fields: []
      action_elements:
        - element: "保存到相册按钮"
          action: "调用 wx.saveImageToPhotosAlbum 保存海报"
          condition: "海报已生成时可点击；保存中时禁用"
        - element: "分享给好友按钮"
          action: "调用 wx.shareFileMessage 分享海报图片"
          condition: "海报已生成时可点击"
        - element: "关闭按钮（右上角X）"
          action: "关闭海报预览弹窗"
          condition: "始终可点击"
        - element: "遮罩层点击区域"
          action: "关闭海报预览弹窗"
          condition: "始终可点击"
    screen_permission_guide_modal:
      display_fields:
        - "提示文案：请在设置中开启相册权限"
      input_fields: []
      action_elements:
        - element: "去设置按钮"
          action: "调用 wx.openSetting 跳转小程序权限设置页"
          condition: "始终可点击"
        - element: "取消按钮"
          action: "关闭权限引导弹窗，返回海报预览弹窗"
          condition: "始终可点击"
```

### 7.3 接口性能基线

| 指标 | 基线值 | 目标值 | 测量条件 | 测量工具 |
|------|--------|--------|----------|----------|
| wxacode 接口响应时间（首次） | 不适用（新接口） | < 3 秒 | 单用户请求，4G 网络 | 后端集成测试计时 |
| wxacode 接口响应时间（缓存命中） | 不适用（新接口） | < 200 毫秒 | 单用户请求 | 后端单元测试计时 |
| 海报生成总耗时（前端） | 不适用（新功能） | < 5 秒 | 包含接口请求+图片下载+Canvas绘制+导出 | 前端 console.time 计时 |
| 海报图片文件大小 | 不适用（新功能） | < 500 KB | 750x1334 分辨率 JPEG 导出 | 文件系统检查 |

### 7.4 原型引用

**无设计稿/原型**。UI 实现参考现有活动详情页的 Claymorphism 蓝紫色系设计风格（见 `miniprogram/pages/activity/activity.wxss`），主色为 `#6366F1`，圆角卡片 + 柔和阴影风格。海报布局参考标准的小程序分享海报模式（上方活动图 + 中间信息 + 下方小程序码）。

## 8. 不确定性与待确认项

| 编号 | 问题 | 影响范围 | 建议 |
|------|------|----------|------|
| UNC-001 | 微信 `wx.shareFileMessage` API 是否在目标基础库版本中可用？该 API 从基础库 2.28.0 开始支持。若不可用需改用 `onShareAppMessage` + `imageUrl` 方案 | REQ-PST-008（海报分享） | 确认小程序 `app.json` 中的 `"lib"` 最低版本配置，或改用 `wx.showShareImageMenu` 作为兼容方案 |
| UNC-002 | 微信 `wxacode.getUnlimited` API 的 `scene` 参数是否受 32 字节限制？当 userId 和 activityId 均为较大数字时，`shareFrom=12345&actId=67890` 可能超过限制 | REQ-PST-009, REQ-PST-012, REQ-PST-013, BS-013 | 建议使用缩写格式 `s={userId}&a={actId}` 缩短参数长度 |
| UNC-003 | 后端当前未实现微信 access_token 的获取和管理机制。调用 `wxacode.getUnlimited` 需要 access_token | REQ-PST-009（后端小程序码生成） | 需新增 access_token 获取与缓存逻辑（有效期 2 小时），可参考现有 UserService 中的微信 API 调用模式 |
| UNC-004 | 海报中是否需要显示活动状态标签（进行中/即将开始/已结束）？需求输入中未明确提及 | REQ-PST-001（海报内容元素） | 建议包含活动状态标签以保持与活动详情页的信息一致性 |
| UNC-005 | 海报背景图是作为全屏背景还是作为上半部分区域？需求输入中未明确海报排版布局细节 | REQ-PST-001, REQ-PST-020 | 建议采用上半部分（约 60%）为活动封面图、下半部分为白色信息区+小程序码的经典布局 |
| UNC-006 | 是否需要在海报中展示用户的个人信息（头像、昵称）以增强分享辨识度？ | REQ-PST-001 | 如需包含，需注意用户隐私授权问题；建议 MVP 版本不包含用户个人信息 |
| UNC-007 | 海报下方"保存到相册"和"分享给好友"两个按钮，是否还需要"分享到朋友圈"独立按钮？当前方案是保存到相册后用户手动发朋友圈 | REQ-PST-008 | 微信小程序不支持直接分享图片到朋友圈，需保存后手动发布，当前方案已是最优路径 |

## 9. 术语表

| 术语 | 定义 |
|------|------|
| Canvas 2D API | 微信小程序提供的 2D 画布绘图接口，用于在小程序端绘制图形和图片，替代旧版 `wx.createCanvasContext` |
| wxacode.getUnlimited | 微信开放接口，用于生成无限数量的小程序码（二维码），支持通过 scene 参数携带自定义数据，适用于动态场景 |
| scene 参数 | 小程序码中携带的自定义数据字段，最大长度 32 字节，用于在用户扫码进入小程序时传递上下文信息 |
| access_token | 微信公众平台的全局接口调用凭据，有效期 2 小时，调用微信服务端 API 时必须携带 |
| shareImage | 活动实体的分享专用图片字段（`activity.share_image`），用于分享场景的展示图，区别于活动封面图 |
| coverImg | 活动实体的封面图字段（`activity.cover_img`），用于活动列表和详情页的展示 |
| studio_name | 画室配置表（`studio_config`）中的画室名称配置项，默认值为"舒然画室" |
| tempFilePath | 微信小程序 Canvas 导出图片后生成的临时文件路径，可用于保存、分享等操作 |
| Claymorphism | 当前小程序采用的 UI 设计风格，特征为柔和阴影、半透明背景、圆角卡片，主色为蓝紫色系 #6366F1 |
| LRU (Least Recently Used) | 最近最少使用淘汰策略，当缓存达到容量上限时，优先移除最久未被访问的条目 |

## 10. 自检清单执行结果

### 完整性检查
- [x] 每条用户故事都有至少一条 EARS 格式需求对应（US-001 -> REQ-PST-001~004, US-002 -> REQ-PST-005~007, US-003 -> REQ-PST-008, US-004 -> REQ-PST-012~013）
- [x] 每条功能需求都有至少一组 Gherkin 验收标准对应（见 7.2 requirement_coverage 逐条映射）
- [x] 边界场景覆盖了 7 个类别中的相关项（空值: BS-001~003, 极值: BS-004~006/BS-018, 并发: BS-012, 权限: BS-010~011, 网络: BS-007~009, 数据完整性: BS-013/BS-017, 状态流转: BS-014~016）
- [x] UI 需求包含完整的成功路径（6.1）和错误路径（6.2）
- [x] 混合需求已拆分为功能子需求、UI 子需求、后端接口子需求，每个子需求有完整验收策略
- [x] 每条需求都有对应的验收策略（7.1 按子需求类型选择模板）
- [x] 技术优化需求的验收策略包含具体的度量指标、目标值和测量工具（7.3 接口性能基线）
- [x] coverage_checklist 穷举到字段级别（7.2 field_coverage 逐字段列出）
- [x] UI 需求的每个 screen 都列出了 display_fields / input_fields / action_elements（7.2 screen_coverage 三个屏幕）
- [x] 功能需求的 field_coverage 逐实体逐字段标注了校验规则和显示格式

### 无歧义检查
- [x] 不存在模糊词（"快速"、"友好"、"高效"、"合理"、"适当"、"大量"、"少量"、"尽快"、"灵活"均未使用）
- [x] 所有度量指标都有具体数值（分辨率 750x1334、响应时间 3秒/200毫秒/5秒、缓存容量 500 条、标题截断 40 字）
- [x] 所有状态转换都有明确的触发条件和目标状态（6.3 状态矩阵逐行定义）
- [x] 所有错误处理都有明确的错误类型和响应行为（6.2 错误路径表格逐条定义）

### 可判定性检查
- [x] 每条验收标准只有"通过"或"不通过"两种判定结果
- [x] 每条验收标准可由第三方独立验证
- [x] Gherkin Then 步骤只描述可观察结果（如"系统显示海报预览弹窗"），不描述内部实现

### 一致性检查
- [x] 需求编号无重复、无断号（REQ-PST-001 ~ REQ-PST-014, REQ-PST-020 ~ REQ-PST-023）
- [x] 同一概念在全文中使用同一术语（术语表已定义 10 个核心术语）
- [x] 用户故事、EARS 需求、Gherkin 场景三者语义一致
- [x] 边界场景的预期行为与对应需求描述一致

### 追溯性检查
- [x] 每条 Gherkin Scenario 标注了关联的需求编号（注释格式 `# 关联需求: REQ-PST-xxx`）
- [x] 每条边界场景标注了关联的需求编号（边界场景清单第 4 列）
- [x] 推断需求与明确需求已区分（UNC-004/005/006 为分析师识别的待确认项，已单独标注在不确定性章节）
