---
title: "full-spec-gen 影响分析报告（积分兑换 + 课程介绍 + 分享海报）"
change_id: "full-spec-gen"
requirement_source:
  - "hhspec/specs/requirements/EXC/REQ-EXC-001.md"
  - "hhspec/specs/requirements/CRS/REQ-CRS-001.md"
  - "hhspec/specs/requirements/PST/REQ-PST-001.md"
date: "2026-03-26"
author: "impact_analyzer-agent"
status: "draft"
risk_level: "中"
affected_specs_count: 3
bootstrap_mode: false
---

# full-spec-gen 影响分析报告

## 1. 分析输入

### 1.1 需求摘要

本次变更包含三个新功能需求：

| 需求 | 领域 | 核心实体 | 核心操作 | 核心数据流 |
|------|------|----------|----------|-----------|
| **积分兑换系统 (EXC)** | EXC | ExchangeItem, ExchangeRecord, User.points | 商品列表查询、积分兑换、兑换记录查询、兑换码核销、管理端 CRUD | 用户积分(USR) -> 兑换操作(EXC) -> 扣减积分+扣减库存+生成记录 -> 兑换码核销(ADM) |
| **课程介绍系统 (CRS)** | CRS | Course | 课程列表展示、分类筛选、课程详情查看、管理端 CRUD | 管理员创建课程(ADM) -> 课程存储(CRS) -> 用户浏览(前端) |
| **分享海报生成 (PST)** | PST | 无新实体（依赖 Activity, StudioConfig） | 前端 Canvas 绘制海报、后端小程序码生成、扫码着陆解析 | 活动数据(ACT)+画室名(STU) -> Canvas绘制 -> 海报图片 -> 扫码 -> scene解析 -> 活动详情页 |

### 1.2 分析范围（specs 全集概况）

specs 目录现有文档：

| 路径 | 类型 | 领域 |
|------|------|------|
| `hhspec/specs/architecture/domain-model.md` | 架构文档 | 全局 |
| `hhspec/specs/requirements/EXC/REQ-EXC-001.md` | 需求文档 | EXC（本次新增） |
| `hhspec/specs/requirements/CRS/REQ-CRS-001.md` | 需求文档 | CRS（本次新增） |
| `hhspec/specs/requirements/PST/REQ-PST-001.md` | 需求文档 | PST（本次新增） |
| `hhspec/domains.yml` | 领域配置 | 全局 |

现有系统领域拓扑（基于 `domains.yml` 和代码扫描）：

- **已实现领域**: USR（用户管理）、ACT（活动管理）、LOT（抽奖系统）、SHR（分享裂变）、TCH（师资管理）、STU（画室配置）、ADM（管理后台）
- **Schema已有/API未实现**: EXC（积分兑换） -- 数据库表和实体类已存在，Mapper 已存在，无 Controller/Service
- **纯规划**: CRS（课程管理）、PST（分享海报）

---

## 2. 上游追溯

### 2.1 直接依赖

#### 2.1.1 积分兑换系统 (EXC) 的直接依赖

| 依赖方 | 被依赖领域 | 依赖内容 | 依赖类型 | 置信度 | 证据来源 |
|--------|-----------|----------|----------|--------|----------|
| EXC | **USR** | 读取 `user.points` 进行积分余额校验；兑换成功后写入 `user.points` 扣减积分 | 数据依赖（读+写） | 高 | REQ-EXC-003/004："用户 points >= 商品 pointsCost"、"将用户 points 减少该商品的 pointsCost"；`mysql/init.sql` 第 12 行 `points INT DEFAULT 0` |
| EXC | **USR** | 读取 `user.id` 关联兑换记录的 `userId`；读取 `user.nick_name` 在核销时返回 | 数据依赖（读） | 高 | REQ-EXC-004："创建一条 exchange_record 记录（包含 userId）"；API 契约 `POST /api/admin/exchange/verify` 返回 `userName` |
| EXC | **ADM** | 管理端接口依赖 AdminInterceptor 的白名单鉴权机制 | 功能依赖 | 高 | REQ-EXC-033："管理端 API 应额外要求管理员白名单权限"；`WebMvcConfig.java` 第 39-40 行 `adminInterceptor.addPathPatterns("/api/admin/**")` |
| EXC | **ADM** | AuthInterceptor 的 JWT 认证机制 | 功能依赖 | 高 | REQ-EXC-033："除商品列表查询外应要求 JWT 认证"；`AuthInterceptor.java` |
| EXC | **LOT** | 积分来源为抽奖系统发放（间接依赖，EXC 本身不调用 LOT，但积分数据由 LOT 产生） | 数据来源依赖 | 高 | REQ-EXC-001 背景："用户通过抽奖获得积分奖品（5/20/50/100积分）"；`LotteryService.java` 第 120-123 行 `user.setPoints(user.getPoints() + selectedPrize.getValue())` |
| EXC | 基础设施 | 复用 `CodeGenerator.generateClaimCode()` 生成兑换码 | 工具依赖 | 高 | REQ-EXC-031："由 CodeGenerator.generateClaimCode() 方法生成"；`CodeGenerator.java` 第 17-19 行 |

#### 2.1.2 课程介绍系统 (CRS) 的直接依赖

| 依赖方 | 被依赖领域 | 依赖内容 | 依赖类型 | 置信度 | 证据来源 |
|--------|-----------|----------|----------|--------|----------|
| CRS | **ADM** | 管理端课程 CRUD 依赖 AdminInterceptor 鉴权 | 功能依赖 | 高 | REQ-CRS-001 覆盖度清单："AdminController 扩展"；现有模式 `WebMvcConfig.java` 第 39-40 行 |
| CRS | **STU** | 首页课程入口区域需在现有首页中嵌入，首页已加载 StudioConfig | UI 依赖 | 高 | EARS-CRS-004："在首页展示课程快捷入口区域"；`index.wxml` 第 47-65 行（功能入口区） |

#### 2.1.3 分享海报生成 (PST) 的直接依赖

| 依赖方 | 被依赖领域 | 依赖内容 | 依赖类型 | 置信度 | 证据来源 |
|--------|-----------|----------|----------|--------|----------|
| PST | **ACT** | 读取 Activity 实体的 `coverImg`、`shareImage`、`title`、`startTime`、`endTime` 绘制海报 | 数据依赖（读） | 高 | REQ-PST-001/002；`Activity.java` 第 14-15 行 `coverImg`、`shareImage` |
| PST | **STU** | 读取 `studio_config` 表的 `studio_name` 显示画室名称 | 数据依赖（读） | 高 | REQ-PST-001："画室名称"；`mysql/init.sql` 第 162 行 `studio_name` |
| PST | **USR** | 需要用户登录（JWT）才能请求小程序码接口；小程序码 scene 中包含 `userId` | 功能+数据依赖 | 高 | REQ-PST-009："scene=shareFrom={userId}&actId={activityId}"；`AuthInterceptor.java` |
| PST | **SHR** | 扫码着陆后 `shareFrom` 参数传递给活动详情页，复用现有分享追踪机制 | 功能依赖 | 中 | REQ-PST-013："将 shareFrom 值传递给活动详情页，用于后续分享追踪"；`activity.js` 第 21-23 行现有 `shareCode` 处理逻辑 |
| PST | 微信API | 调用 `wxacode.getUnlimited` 需要 access_token（当前系统未实现） | 服务依赖 | 高 | REQ-PST-009；UNC-003："后端当前未实现微信 access_token 的获取和管理机制" |

### 2.2 间接依赖

| 依赖链 | 说明 | 深度 | 置信度 |
|--------|------|------|--------|
| EXC -> USR.points -> LOT（积分发放） | 积分兑换的资金来源完全依赖抽奖系统发放的积分 | 2 层 | 高 |
| EXC -> USR.points -> LOT -> SHR（分享获得抽奖机会） | 更深层：用户需要先分享获得抽奖机会，再抽奖获得积分，最终在兑换系统消费 | 3 层 | 高 |
| PST -> ACT -> SHR（分享追踪闭环） | 海报扫码进入活动详情页后，shareFrom 参数进入现有分享追踪流程 | 2 层 | 中 |
| CRS -> ADM -> AdminWhitelist | 课程管理依赖管理员身份，管理员身份由白名单表控制 | 2 层 | 高 |

---

## 3. 下游扩散

### 3.1 直接影响

#### 3.1.1 积分兑换系统 (EXC) 对已有系统的影响

| 受影响模块 | 影响描述 | 影响类型 | 置信度 | 证据来源 |
|-----------|----------|----------|--------|----------|
| **首页 (index)** | 功能入口区域需新增"积分商城"入口 | compatible | 高 | REQ-EXC-022："在首页功能入口区域提供进入积分商城的导航入口"；`index.wxml` 第 48-65 行现有 4 个入口 |
| **"我的"页面 (my)** | 菜单列表需新增"积分商城"和"兑换记录"入口 | compatible | 高 | REQ-EXC-022："在'我的'页面菜单中提供进入积分商城的导航入口"；`my.wxml` 第 26-42 行菜单列表 |
| **管理后台首页 (admin/index)** | 菜单网格需新增"兑换商品"和"兑换核销"管理入口 | compatible | 高 | REQ-EXC-016~021 管理端功能；`admin/index/index.wxml` 第 7-24 行菜单网格 |
| **WebMvcConfig** | 需将 `GET /api/exchange/items` 加入 auth excludePathPatterns（公开接口） | compatible | 高 | REQ-EXC-033："商品列表查询无需认证"；`WebMvcConfig.java` 第 29-36 行 excludePathPatterns |
| **app.json** | 需新增积分商城、兑换记录、管理端兑换商品、管理端核销 4 个页面路径 | compatible | 高 | 现有 `app.json` 第 2-13 行 pages 数组 |
| **AdminController / AdminService** | 需新增兑换商品 CRUD 和兑换码核销接口（或新建 ExchangeController） | additive | 高 | 需求 API 契约：`GET/POST/DELETE /api/admin/exchange-item[s]`、`POST /api/admin/exchange/verify` |
| **User.points 写操作** | 兑换操作新增一个积分扣减路径（与 LOT 的积分增加路径并存） | compatible | 高 | `domain-model.md` 第 105 行："用户积分只通过两个途径变更：抽奖中积分奖品（LOT写入）、积分兑换（EXC扣减）" |

#### 3.1.2 课程介绍系统 (CRS) 对已有系统的影响

| 受影响模块 | 影响描述 | 影响类型 | 置信度 | 证据来源 |
|-----------|----------|----------|--------|----------|
| **首页 (index)** | 需新增课程快捷入口区域（推荐课程卡片）；功能入口可能需新增"课程介绍"入口 | compatible | 高 | EARS-CRS-004："在首页展示课程快捷入口区域"；`index.wxml` 第 47-65 行、第 67-83 行 |
| **管理后台首页 (admin/index)** | 菜单网格需新增"课程管理"入口 | compatible | 高 | REQ-CRS-001 覆盖度清单："管理后台课程管理页 /pages/admin/courses/courses" |
| **WebMvcConfig** | 需将 `GET /api/course/list` 和 `GET /api/course/*` 加入 auth excludePathPatterns | compatible | 高 | REQ-CRS-001 覆盖度清单："GET /api/course/list (公开), GET /api/course/{id} (公开)" |
| **app.json** | 需新增课程列表页、课程详情页（或复用弹窗）、管理端课程管理页路径 | compatible | 高 | REQ-CRS-001 覆盖度清单 |
| **mysql/init.sql** | 需新增 `course` 建表语句 | additive | 高 | REQ-CRS-001 覆盖度清单："course 数据库表 新增" |
| **AdminController / AdminService** | 需新增课程 CRUD 接口 | additive | 高 | REQ-CRS-001："AdminController 扩展 GET/POST/DELETE /api/admin/course[s]" |

#### 3.1.3 分享海报生成 (PST) 对已有系统的影响

| 受影响模块 | 影响描述 | 影响类型 | 置信度 | 证据来源 |
|-----------|----------|----------|--------|----------|
| **活动详情页 (activity)** | 底部操作栏需新增"生成海报"按钮（现有 2 个按钮变为 3 个） | compatible | 高 | REQ-PST-001；`activity.wxml` 第 52-61 行现有 action-bar 含"分享活动"和"立即抽奖"两个按钮 |
| **活动详情页 (activity.js)** | 需新增海报生成、Canvas 绘制、保存相册、分享好友的完整逻辑 | compatible | 高 | REQ-PST-001~008, REQ-PST-014；`activity.js` 需大量新增代码 |
| **活动详情页 (activity.wxml)** | 需新增隐藏 Canvas 元素、海报预览弹窗、权限引导弹窗 | compatible | 高 | 6.3 状态矩阵定义了 5 个页面状态 |
| **活动详情页 (activity.wxss)** | 需新增海报弹窗、Canvas、按钮样式 | compatible | 高 | 海报预览弹窗的遮罩层、居中布局等 CSS |
| **ShareController / ShareService** | 需新增 `GET /api/share/wxacode` 接口用于生成小程序码 | additive | 高 | REQ-PST-009；扩展现有 ShareController |
| **小程序入口 (app.js)** | 需处理扫码启动时的 scene 参数解析和页面跳转 | compatible | 中 | REQ-PST-012/013："解析 scene 参数中的 actId"；需在 `onLaunch` 或 `onShow` 中增加 scene 解析逻辑 |
| **UserService** | 需新增微信 access_token 获取和缓存机制，供 wxacode API 调用 | additive | 高 | UNC-003："需新增 access_token 获取与缓存逻辑"；`UserService.java` 已有 `appid`/`secret` 配置（第 29-31 行） |

### 3.2 级联影响

| 传播链 | 影响层数 | 说明 | 置信度 |
|--------|---------|------|--------|
| EXC 新增首页入口 -> index.wxml 布局变化 -> CRS 也需新增首页入口 -> 首页功能入口区需同时容纳两个新入口 | 1 层 | EXC 和 CRS 同时要求在首页新增入口，功能入口区从 4 个变为 5-6 个，需要重新设计布局 | 高 |
| PST 新增活动页按钮 -> activity.wxml 底部操作栏变化 -> 按钮布局从 2 个变 3 个 | 1 层 | 仅影响活动详情页 CSS 布局 | 高 |
| EXC 扣减 User.points -> 影响"我的"页面积分显示 | 0 层（已有） | `my.wxml` 第 12 行已显示 `userInfo.points`，兑换后积分变化自动反映 | 高 |

### 3.3 影响类型分布

| 影响类型 | 数量 | 涉及模块 |
|---------|------|---------|
| `breaking` | 0 | 无 |
| `compatible` | 12 | 首页、"我的"页面、活动详情页、管理后台首页、WebMvcConfig、app.json、app.js 等 |
| `additive` | 5 | AdminController 扩展、ShareController 扩展、UserService 扩展、init.sql 新增表、新页面文件 |

本次三个需求均为纯新增功能，不存在破坏性变更。所有对已有文件的修改均为添加新入口/按钮/配置，不修改已有接口签名或数据模型结构。

---

## 4. 流程链分析

### 4.1 受影响的业务流程清单

| 流程编号 | 流程名称 | 涉及新需求 | 影响类型 |
|---------|---------|-----------|----------|
| **BF-001** | 积分消费闭环（新增流程） | EXC | 新增流程 |
| **BF-002** | 用户激励完整链路 | EXC | 在已有链路末端新增消费环节 |
| **BF-003** | 首页导航流程 | EXC + CRS | 已有流程新增分叉路径 |
| **BF-004** | 管理后台操作流程 | EXC + CRS | 已有流程新增分叉路径 |
| **BF-005** | 课程展示流程（新增流程） | CRS | 新增流程 |
| **BF-006** | 活动分享传播流程 | PST | 已有流程新增并行路径 |
| **BF-007** | 扫码着陆流程 | PST | 已有流程扩展（新增小程序码入口） |
| **BF-008** | "我的"页面信息展示 | EXC | 已有流程新增菜单入口 |

### 4.2 流程触点详情

#### BF-002: 用户激励完整链路（现有 + EXC 扩展）

```
[现有] 分享活动(SHR) -> 好友确认 -> 分享者获得抽奖机会(USR.lotteryChances+1)
[现有]                                                     -> 抽奖(LOT) -> 中积分奖品 -> 用户积分增加(USR.points+N)
[新增 EXC]                                                                                -> 积分商城 -> 兑换商品 -> 积分扣减(USR.points-N) -> 生成兑换码/自动完成
[新增 EXC]                                                                                                                                    -> 到店核销(ADM)
```

**触点**: EXC 在 `USR.points` 链路末端新增消费出口。不中断已有流程任何环节。

#### BF-003: 首页导航流程

```
用户进入首页 -> 查看画室信息 -> [现有] 功能入口区（师资/活动/抽奖/分享） -> 各功能页
                                [新增 EXC] -> "积分商城" 入口 -> /pages/exchange/exchange
                                [新增 CRS] -> "课程介绍" 入口 -> /pages/courses/courses
                            -> [新增 CRS] 课程推荐卡片区 -> 课程列表页
                            -> [现有] 活动列表 -> 活动详情页
```

**触点**: 首页功能入口区从 4 个扩展为 5-6 个；首页内容区新增课程推荐卡片。需重新设计功能入口区布局。

#### BF-006: 活动分享传播流程

```
用户进入活动详情页 -> [现有] 点击"分享活动" -> 微信原生转发 -> 好友在聊天中点击 -> 进入活动详情页
                   -> [新增 PST] 点击"生成海报" -> 获取小程序码(API) -> Canvas绘制海报 -> 海报预览弹窗
                                                                                       -> 保存到相册 -> 用户发朋友圈/其他平台
                                                                                       -> 分享给好友 -> 微信好友收到图片
                                                 扫码海报 -> 解析scene -> 跳转活动详情页(携带shareFrom)
```

**触点**: PST 在活动详情页底部操作栏新增"生成海报"按钮，与现有"分享活动"形成并行分享路径。不中断现有分享流程。

#### BF-004: 管理后台操作流程

```
管理员进入管理后台 -> [现有] 画室配置 / 老师管理 / 活动管理 / 奖品管理
                   -> [新增 EXC] 兑换商品管理 -> CRUD 兑换商品
                   -> [新增 EXC] 兑换核销 -> 输入兑换码核销
                   -> [新增 CRS] 课程管理 -> CRUD 课程
```

**触点**: 管理后台首页菜单网格从 4 个扩展为 6-7 个。

### 4.3 跨流程关联

| 共享实体/状态 | 关联流程 | 关联说明 |
|-------------|---------|---------|
| `User.points` | BF-002（积分获取） + BF-001（积分消费） | LOT 写入积分、EXC 扣减积分，并发场景下需确保积分不为负 |
| `Activity` 实体 | BF-006（海报分享） + BF-007（扫码着陆） + 现有活动详情 | PST 读取活动信息绘制海报，扫码后跳转活动详情 |
| 首页布局 | BF-003（首页导航） | EXC 和 CRS 同时要求新增首页入口，需统一规划 |
| 管理后台布局 | BF-004（管理操作） | EXC 和 CRS 同时要求新增管理入口，需统一规划 |

---

## 5. 变更范围界定

### 5.1 受影响 Spec 索引

```yaml
affected_specs:
  # === 积分兑换系统 (EXC) ===
  - spec_id: "domain-model"
    spec_path: "hhspec/specs/architecture/domain-model.md"
    sections:
      - section: "3.6 兑换上下文 (EXC)"
        description: "需将'已规划未实现'更新为'已实现'，补充完整的领域描述，包括兑换码核销流程和管理端 CRUD"
      - section: "4.1 依赖矩阵"
        description: "EXC 行的 USR 列已标注依赖，需确认无变化"
      - section: "5.1 用户端页面结构"
        description: "需新增积分商城页(/pages/exchange/exchange)和兑换记录页(/pages/exchange/records)"
      - section: "5.2 管理端页面结构"
        description: "需新增兑换商品管理页(/pages/admin/exchange/exchange)和兑换核销页"
      - section: "6. API 端点概览"
        description: "需新增 EXC 相关的 3 个用户端点和 4 个管理端点"
    change_type: "modify"
    impact_type: "compatible"
    confidence: "高"
    risk_level: "低"
    rationale: "domain-model.md 第 77 行已标注 EXC 为'已规划未实现'，需更新状态并补充页面和 API 清单"

  - spec_id: "domains-yml"
    spec_path: "hhspec/domains.yml"
    sections:
      - section: "EXC 领域配置"
        description: "需将 status 从 schema_only 更新为 implemented；补充 API 端点信息"
    change_type: "modify"
    impact_type: "compatible"
    confidence: "高"
    risk_level: "低"
    rationale: "domains.yml 第 70 行 status: schema_only 需更新"

  # === 课程介绍系统 (CRS) ===
  - spec_id: "domain-model-crs"
    spec_path: "hhspec/specs/architecture/domain-model.md"
    sections:
      - section: "2. 限界上下文"
        description: "需在画室上下文中新增 Course 实体"
      - section: "3. 核心概念"
        description: "需新增 '3.X 课程上下文 (CRS)' 小节，描述 Course 实体和业务规则"
      - section: "4.1 依赖矩阵"
        description: "需新增 CRS 行，标注其对 ADM 的依赖"
      - section: "5.1 用户端页面结构"
        description: "需新增课程列表页和课程详情页"
      - section: "5.2 管理端页面结构"
        description: "需新增课程管理页"
      - section: "6. API 端点概览"
        description: "需新增 CRS 相关公开端点和管理端点"
    change_type: "extend"
    impact_type: "additive"
    confidence: "高"
    risk_level: "低"
    rationale: "domain-model.md 当前无 CRS 相关内容（仅 domains.yml 第 73-80 行有规划）"

  - spec_id: "domains-yml-crs"
    spec_path: "hhspec/domains.yml"
    sections:
      - section: "CRS 领域配置"
        description: "需将 status 从 planned 更新为 implemented"
    change_type: "modify"
    impact_type: "compatible"
    confidence: "高"
    risk_level: "低"
    rationale: "domains.yml 第 80 行 status: planned 需更新"

  # === 分享海报生成 (PST) ===
  - spec_id: "domain-model-pst"
    spec_path: "hhspec/specs/architecture/domain-model.md"
    sections:
      - section: "3.4 分享上下文 (SHR)"
        description: "需补充 PST 海报生成子功能描述，包括小程序码生成和 Canvas 绘制"
      - section: "4.1 依赖矩阵"
        description: "需新增 PST 行或在 SHR 行标注对 ACT/STU 的读依赖"
      - section: "6.2 认证端点"
        description: "需新增 GET /api/share/wxacode 端点"
    change_type: "extend"
    impact_type: "additive"
    confidence: "高"
    risk_level: "低"
    rationale: "domain-model.md 第 69 行分享上下文仅描述 ShareRecord，需扩展 PST 功能"

  - spec_id: "domains-yml-pst"
    spec_path: "hhspec/domains.yml"
    sections:
      - section: "PST 领域配置"
        description: "需将 status 从 planned 更新为 implemented"
    change_type: "modify"
    impact_type: "compatible"
    confidence: "高"
    risk_level: "低"
    rationale: "domains.yml 第 88 行 status: planned 需更新"
```

### 5.2 变更执行顺序

基于依赖关系，变更的推荐执行顺序：

```
第 1 批（无跨领域依赖，可并行）:
  1a. CRS 课程介绍系统 -- 完全独立新增，不依赖其他新功能
  1b. PST 分享海报生成 -- 仅依赖已有的 ACT/STU/USR，不依赖其他新功能

第 2 批（依赖已有系统的积分数据）:
  2.  EXC 积分兑换系统 -- 虽然不依赖 CRS/PST，但涉及 User.points 写操作和
      CodeGenerator 复用，建议在充分测试积分流转后实施

第 3 批（跨功能集成）:
  3a. 首页布局统一调整 -- 同时容纳 EXC 和 CRS 的新入口
  3b. 管理后台布局统一调整 -- 同时容纳 EXC 和 CRS 的新管理入口
  3c. domain-model.md 统一更新 -- 一次性更新所有领域变更
  3d. domains.yml 统一更新 -- 一次性更新所有状态
```

### 5.3 新增 Spec 清单

三个需求文档已在 specs 目录中创建，无需额外新增 spec 文件。后续如果需要为每个领域创建独立的 API 契约 spec 或数据模型 spec，可在设计阶段新增。

### 5.4 需要修改的已有代码文件

#### 后端文件

| 文件路径 | 变更类型 | 变更内容 | 涉及需求 |
|---------|---------|---------|---------|
| `backend/.../config/WebMvcConfig.java` | modify | excludePathPatterns 新增 `/api/exchange/items`、`/api/course/list`、`/api/course/*` | EXC, CRS |
| `backend/.../controller/AdminController.java` | modify | 新增课程 CRUD 端点（或拆分到新 Controller） | CRS |
| `backend/.../service/AdminService.java` | modify | 新增课程 CRUD 方法 | CRS |
| `backend/.../controller/ShareController.java` | modify | 新增 `GET /api/share/wxacode` 端点 | PST |
| `backend/.../service/ShareService.java` | modify | 新增小程序码生成+缓存逻辑 | PST |
| `backend/.../service/UserService.java` | modify | 新增 access_token 获取与缓存逻辑（或拆分到新 WxApiService） | PST |
| `mysql/init.sql` | modify | 新增 `course` 建表语句 | CRS |

#### 前端文件

| 文件路径 | 变更类型 | 变更内容 | 涉及需求 |
|---------|---------|---------|---------|
| `miniprogram/app.json` | modify | pages 数组新增 6-8 个新页面路径 | EXC, CRS |
| `miniprogram/pages/index/index.wxml` | modify | 功能入口区新增"积分商城"和"课程介绍"入口；内容区新增课程推荐卡片 | EXC, CRS |
| `miniprogram/pages/index/index.js` | modify | 新增 goToExchange()、goToCourses() 方法；loadData 新增课程数据加载 | EXC, CRS |
| `miniprogram/pages/index/index.wxss` | modify | 功能入口区布局调整（4->5/6 个） | EXC, CRS |
| `miniprogram/pages/my/my.wxml` | modify | 菜单列表新增"积分商城"和"兑换记录"入口 | EXC |
| `miniprogram/pages/my/my.js` | modify | 新增 goToExchange()、goToExchangeRecords() 方法 | EXC |
| `miniprogram/pages/activity/activity.wxml` | modify | 底部操作栏新增"生成海报"按钮；新增 Canvas 元素和海报预览弹窗 | PST |
| `miniprogram/pages/activity/activity.js` | modify | 新增海报生成、Canvas 绘制、保存相册、分享好友的完整逻辑；scene 解析逻辑 | PST |
| `miniprogram/pages/activity/activity.wxss` | modify | 新增海报弹窗、Canvas、三按钮布局样式 | PST |
| `miniprogram/pages/admin/index/index.wxml` | modify | 菜单网格新增"兑换商品"、"兑换核销"、"课程管理"入口 | EXC, CRS |
| `miniprogram/pages/admin/index/index.js` | modify | 新增 goToExchangeItems()、goToExchangeVerify()、goToCourses() 方法 | EXC, CRS |

### 5.5 需要新增的代码文件

#### 后端新增文件

| 文件路径 | 涉及需求 | 说明 |
|---------|---------|------|
| `backend/.../controller/ExchangeController.java` | EXC | 用户端兑换 API（商品列表/兑换/记录） |
| `backend/.../service/ExchangeService.java` | EXC | 兑换业务逻辑（积分校验/库存扣减/事务） |
| `backend/.../entity/Course.java` | CRS | 课程实体类 |
| `backend/.../mapper/CourseMapper.java` | CRS | 课程数据访问 |
| `backend/.../controller/CourseController.java` | CRS | 用户端课程 API（列表/详情） |
| `backend/.../service/CourseService.java` | CRS | 课程业务逻辑 |
| `mysql/migration_course.sql` | CRS | course 表建表迁移脚本 |

#### 前端新增文件

| 文件路径 | 涉及需求 | 说明 |
|---------|---------|------|
| `miniprogram/pages/exchange/exchange.{js,wxml,wxss,json}` | EXC | 积分商城页 |
| `miniprogram/pages/exchange/records.{js,wxml,wxss,json}` | EXC | 兑换记录页 |
| `miniprogram/pages/admin/exchange/exchange.{js,wxml,wxss,json}` | EXC | 管理端兑换商品管理页 |
| `miniprogram/pages/admin/exchange/verify.{js,wxml,wxss,json}` | EXC | 管理端兑换核销页 |
| `miniprogram/pages/courses/courses.{js,wxml,wxss,json}` | CRS | 课程列表页 |
| `miniprogram/pages/courses/detail.{js,wxml,wxss,json}` | CRS | 课程详情页（如不用弹窗） |
| `miniprogram/pages/admin/courses/courses.{js,wxml,wxss,json}` | CRS | 管理端课程管理页 |

---

## 6. 风险评估

### 6.1 回归风险矩阵

| 受影响领域/模块 | 影响范围(30%) | 变更深度(25%) | 耦合程度(20%) | 测试覆盖(15%) | 可逆性(10%) | 加权分 | 风险等级 |
|---------------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **User.points（积分写操作）** | 2 (EXC+LOT 两个消费方) | 2 (数据字段写操作) | 3 (直接调用 UserMapper) | 3 (无自动化测试) | 1 (代码可回滚) | **2.15** | **中** |
| **首页 index** | 2 (EXC+CRS 两个新入口) | 1 (UI 布局调整) | 2 (共享页面) | 3 (无自动化测试) | 1 (代码可回滚) | **1.75** | **中** |
| **活动详情页 activity** | 1 (PST 一个新功能) | 1 (新增按钮和弹窗) | 2 (共享页面) | 3 (无自动化测试) | 1 (代码可回滚) | **1.55** | **中** |
| **管理后台 admin/index** | 2 (EXC+CRS 两个新入口) | 1 (UI 布局调整) | 1 (弱耦合，仅导航) | 3 (无自动化测试) | 1 (代码可回滚) | **1.60** | **中** |
| **WebMvcConfig** | 2 (新增 3 个公开端点) | 1 (配置变更) | 3 (影响全局认证) | 3 (无自动化测试) | 1 (代码可回滚) | **1.95** | **中** |
| **"我的"页面 my** | 1 (EXC 新入口) | 1 (新增菜单项) | 1 (弱耦合) | 3 (无自动化测试) | 1 (代码可回滚) | **1.30** | **低** |
| **app.json** | 0 (纯配置新增) | 0 (纯新增) | 1 (全局路由) | 3 (无自动化测试) | 1 (代码可回滚) | **0.75** | **低** |
| **AdminController/Service** | 2 (EXC+CRS 新增方法) | 1 (纯新增方法) | 2 (共享控制器) | 3 (无自动化测试) | 1 (代码可回滚) | **1.75** | **中** |
| **ShareController/Service** | 1 (PST 新增端点) | 1 (纯新增方法) | 2 (共享控制器) | 3 (无自动化测试) | 1 (代码可回滚) | **1.45** | **低** |
| **mysql/init.sql** | 1 (新增表) | 3 (数据模型变更) | 1 (独立新表) | 3 (无自动化测试) | 2 (schema 变更难回滚) | **1.95** | **中** |

### 6.2 向后兼容性清单

| 接口/数据模型 | 兼容性结论 | 说明 |
|-------------|-----------|------|
| `GET /api/user/info` | **完全兼容** | 返回字段不变，`points` 字段值可能因兑换减少但格式不变 |
| `POST /api/lottery/draw` | **完全兼容** | 积分发放逻辑不变 |
| `POST /api/share/create` | **完全兼容** | 分享创建逻辑不变 |
| `POST /api/share/confirm` | **完全兼容** | 分享确认逻辑不变 |
| `GET /api/share/status` | **完全兼容** | 分享状态查询逻辑不变 |
| `GET /api/activity/list` | **完全兼容** | 活动列表接口不变 |
| `GET /api/activity/{id}` | **完全兼容** | 活动详情接口不变 |
| `GET /api/studio/config` | **完全兼容** | 画室配置接口不变 |
| `GET /api/teacher/list` | **完全兼容** | 教师列表接口不变 |
| `GET /api/lottery/prizes` | **完全兼容** | 奖品池接口不变 |
| `GET /api/lottery/records` | **完全兼容** | 抽奖记录接口不变 |
| `user` 表结构 | **完全兼容** | 不新增/修改任何字段 |
| `activity` 表结构 | **完全兼容** | 不新增/修改任何字段 |
| `exchange_item` 表结构 | **完全兼容** | 表已存在，结构匹配需求 |
| `exchange_record` 表结构 | **完全兼容** | 表已存在，结构匹配需求 |
| 所有管理端 API | **完全兼容** | 新增端点不影响已有端点 |

**结论**: 本次三个需求均为纯新增功能，所有已有接口和数据模型保持完全兼容。无破坏性变更。

### 6.3 高风险项专项分析

本次评估无高风险项（加权分均 < 2.5）。最高风险项为以下两个中风险项：

#### 6.3.1 User.points 并发写操作（加权分 2.15）

**风险描述**: EXC 兑换操作扣减 `user.points`，LOT 抽奖操作增加 `user.points`。若用户同时抽奖和兑换，可能出现积分计算错误。

**现有机制**: `LotteryService.draw()` 使用 `@Transactional`（`LotteryService.java` 第 38 行），但读取后直接 `setPoints` 再 `updateById`，未使用乐观锁或 `UPDATE ... SET points = points - N` 的原子操作。

**缓解措施**:
1. EXC 的兑换操作必须使用 `UPDATE user SET points = points - ? WHERE id = ? AND points >= ?` 的原子 SQL，而非先读后写。
2. 需求 REQ-EXC-030 已要求事务一致性，BD-009 已覆盖并发场景。
3. 建议为 LOT 的积分发放也采用相同原子操作模式（但属于已有系统优化，不在本次范围内）。

#### 6.3.2 WebMvcConfig 公开端点配置（加权分 1.95）

**风险描述**: 新增 3 个无需认证的公开端点，配置错误可能导致需要认证的端点被意外暴露。

**缓解措施**:
1. 仅添加明确的路径模式：`/api/exchange/items`、`/api/course/list`、`/api/course/*`。
2. 需在集成测试中验证：兑换操作 API (`POST /api/exchange/redeem`) 未登录时返回 401。
3. 管理端 API 已由 `adminInterceptor` 独立拦截，不受 auth excludePathPatterns 影响。

---

## 7. 不确定性与待确认项

| 编号 | 问题 | 影响范围 | 置信度 | 建议 |
|------|------|----------|--------|------|
| UC-I-001 | 首页功能入口区从 4 个增至 5-6 个（EXC "积分商城" + CRS "课程介绍"），需确认布局方案：扩展为多行、替换已有入口、还是保持 4 个入口替换方案？ | 首页 index.wxml/wxss、EXC REQ-EXC-022、CRS EARS-CRS-004 | 低 | 建议设计阶段统一规划首页入口布局。REQ-EXC-001 的 UC-006 也提出了此问题。 |
| UC-I-002 | 管理后台首页从 4 个入口增至 6-7 个（+兑换商品、兑换核销、课程管理），菜单网格布局需调整。 | 管理后台 admin/index | 低 | 建议采用 2 行 4 列或分组布局。 |
| UC-I-003 | PST 需求的微信 access_token 获取机制是新增到 UserService 还是新建独立的 WxApiService？当前 UserService 已有 appid/secret 配置但未有 access_token 逻辑。 | PST REQ-PST-009、UserService | 低 | 建议新建 WxApiService 封装 access_token 获取和缓存，避免 UserService 职责膨胀。 |
| UC-I-004 | PST 的小程序码 scene 参数 32 字节限制：当 userId 和 activityId 为大数时 `s={userId}&a={actId}` 是否可能超限？ | PST REQ-PST-009/012/013 | 中 | REQ-PST-001 的 UNC-002 和 BS-013 已识别此问题，建议使用缩写格式。 |
| UC-I-005 | PST 需在 app.js onLaunch 中解析 scene 参数，但当前 app.js 未被分析（未提供文件）。是否已有 scene 解析逻辑？ | PST REQ-PST-012/013 | 中 | 需检查 `miniprogram/app.js` 是否存在 scene 处理逻辑。若无，需新增。 |
| UC-I-006 | EXC 管理端 CRUD 是扩展现有 AdminController（增加方法）还是新建 ExchangeAdminController？AdminController 已有 4 组 CRUD，再增加 2 组会使文件过大。 | EXC REQ-EXC-016~021 | 低 | 建议新建独立的 ExchangeController 处理用户端和管理端请求（通过路径区分 `/api/exchange/*` 和 `/api/admin/exchange*`），保持 AdminController 职责清晰。 |
| UC-I-007 | CRS 课程详情页是独立页面还是在课程列表页内使用弹窗？REQ-CRS-001 覆盖度清单写了"(或复用列表页弹窗)"。 | CRS 前端页面规划 | 低 | 建议设计阶段确认。独立页面更符合小程序导航习惯。 |

---

## 8. 建议与下一步

### 8.1 变更实施建议（优先级排序）

1. **P0 -- 首页/管理后台布局统一规划**: 由于 EXC 和 CRS 同时要求在首页和管理后台新增入口，建议先完成布局设计方案，再分别实施各功能。
2. **P1 -- CRS 课程介绍系统**: 完全独立的新增功能，不涉及积分/并发等复杂场景，可最先开发验证。
3. **P1 -- PST 分享海报生成**: 仅依赖已有 ACT/STU 数据，新增后端接口较少（1 个 wxacode 端点），但前端 Canvas 绘制逻辑较复杂，建议与 CRS 并行开发。
4. **P2 -- EXC 积分兑换系统**: 涉及积分写操作、事务一致性、并发控制，技术复杂度最高。建议在 CRS/PST 完成后再实施，确保有充足时间测试积分流转。

### 8.2 测试策略建议（重点回归范围）

| 回归测试领域 | 测试重点 | 优先级 |
|-------------|---------|--------|
| **积分流转** | 抽奖获得积分 -> 积分余额正确 -> 兑换扣减积分 -> 积分余额正确 -> 并发场景不出现负积分 | P0 |
| **认证鉴权** | 新增公开端点确认无需 token、兑换操作确认需要 token、管理端确认需要管理员权限 | P0 |
| **首页功能** | 新增入口后布局不错位、所有入口导航正确、现有功能（轮播/活动列表/地图）不受影响 | P1 |
| **活动详情页** | 新增"生成海报"按钮后不影响"分享活动"和"立即抽奖"功能 | P1 |
| **管理后台** | 新增管理入口后不影响现有画室配置/教师管理/活动管理/奖品管理功能 | P1 |
| **数据库** | course 表创建不影响现有表；exchange_item/exchange_record 表结构验证 | P2 |

### 8.3 风险缓解措施

| 风险 | 缓解措施 | 负责阶段 |
|------|---------|---------|
| 积分并发写冲突 | EXC 兑换操作使用原子 SQL `UPDATE user SET points = points - ? WHERE id = ? AND points >= ?` | 设计+开发 |
| 首页布局冲突 | EXC 和 CRS 的首页入口在同一个设计任务中统一规划 | 设计 |
| WebMvcConfig 公开端点错配 | 新增端点后编写认证回归测试用例 | 测试 |
| 微信 access_token 管理 | 新建独立 WxApiService 封装，设置 2 小时刷新周期，添加重试机制 | 设计+开发 |
| 小程序码 scene 参数超长 | 统一使用缩写格式 `s={userId}&a={actId}`，开发时验证 32 字节限制 | 开发+测试 |

---

## 9. 自检清单执行结果

### 完整性检查
- [x] 四维分析（上游/下游/流程链/变更范围）全部执行，无遗漏维度
- [x] 每个受影响的 spec 都有完整的索引条目（spec_id + sections + change_type）
- [x] 所有影响类型为 `compatible` 和 `additive`，无 `breaking` 类型（本次纯新增功能）
- [x] 所有中风险项都有专项分析和缓解措施（6.3.1 积分并发、6.3.2 WebMvcConfig）
- [x] 非技术优化类需求，不要求 affected_interfaces 逐接口逐字段标注

### 证据充分性检查
- [x] 每条影响判断都附有 spec 文件路径和具体章节/行号引用
- [x] "确定受影响"与"可能受影响"已通过置信度（高/中/低）明确标注
- [x] 不确定性章节已记录 7 项证据缺口（UC-I-001 ~ UC-I-007）

### 一致性检查
- [x] 上游依赖和下游扩散的方向正确，无逻辑矛盾
- [x] 风险评分与影响类型判断一致（无 breaking 变更，最高为中风险）
- [x] 变更执行顺序尊重依赖关系（CRS/PST 可并行 -> EXC -> 集成调整），无循环依赖

### 可操作性检查
- [x] 变更清单（5.4/5.5）可直接指导后续设��和开发任务拆分
- [x] 测试策略建议（8.2）可直接指导回归测试范围划定
- [x] 风险缓解措施（8.3）具体可执行（原子 SQL、统一布局设计、独立 Service 等）
