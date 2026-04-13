> **逆向生成** — 基于代码分析自动生成，需人工确认和补充。

# 领域模型 - 舒然画室小程序

## 1. 系统概述

舒然画室微信小程序，面向美术培训机构的招生推广工具。核心场景：通过「分享裂变 + 抽奖激励」机制，在开学季、暑假前等节点进行活动推广，吸引潜在学员和家长。

### 技术栈
| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | 微信小程序（原生） | - |
| 后端 | Spring Boot + MyBatis Plus | 3.2.0 / 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 认证 | JWT (JJWT) | 0.12.3 |
| 运行时 | Java | 21 |
| 部署 | Docker Compose + Nginx | - |
| 域名 | tianma.chat | - |

## 2. 限界上下文

```
┌─────────────────────────────────────────────────────────┐
│                     画室上下文                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   画室配置    │  │   师资管理    │  │   课程管理    │  │
│  │  StudioConfig │  │   Teacher    │  │   Course     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   用户上下文   │    │   活动上下文   │    │   抽奖上下文  │
│     User      │←──→│   Activity   │←──→│    Prize     │
│   积分/机会    │    │ ActivityVisit │    │ LotteryRecord│
└──────┬───────┘    └──────┬───────┘    └──────────────┘
       │                   │
       │            ┌──────▼───────┐
       │            │   分享上下文   │
       └───────────→│  ShareRecord  │
                    │  海报生成(PST) │
                    └──────────────┘

┌──────────────┐    ┌──────────────┐
│   兑换上下文   │    │   管理上下文   │
│ ExchangeItem │    │AdminWhitelist│
│ExchangeRecord│    │  (CRUD 管理)  │
└──────────────┘    └──────────────┘
```

## 3. 核心概念

### 3.1 用户上下文 (USR)
- **User**: 微信用户，通过 OpenID 标识。持有积分余额和剩余抽奖次数
- 登录方式：微信 `jscode2session` → JWT Token（7天有效）
- 用户属性：昵称、头像（DiceBear 兜底）、手机号、积分、抽奖次数

### 3.2 活动上下文 (ACT)
- **Activity**: 营销活动，有起止时间、状态（进行中/已结束/未开始）
- 控制参数：每日分享上限 `daily_share_limit`、总分享上限 `total_share_limit`、每人最大抽奖次数 `max_lottery_per_user`
- 自定义分享文案和图片
- **ActivityVisit**: 活动首次访问记录，首次访问赠送1次抽奖机会

### 3.3 抽奖上下文 (LOT)
- **Prize**: 奖品配置。包含类型（积分/体验课/画材礼包）、等级（1-4等奖）、概率、库存
- **LotteryRecord**: 抽奖记录。关联用户、活动、奖品。实物奖品有领取码和过期时间
- **保底机制**: 每8次抽奖保底一次1等奖或2等奖（80%概率2等奖，20%概率1等奖）
- 积分类奖品自动发放到用户余额；实物奖品生成8位领取码，30天有效

### 3.4 分享上下文 (SHR)
- **ShareRecord**: 两阶段分享。分享者发起 → 获得分享码 → 访客点击确认 → 分享者获得抽奖机会
- 防重复：同一访客对同一分享者同一活动只计1次
- 每个活动有总分享次数限制

### 3.5 画室上下文 (STU + TCH)
- **StudioConfig**: Key-Value 配置存储。包括名称、口号、介绍、地址、经纬度、轮播图、二维码、分享模板
- **Teacher**: 教师信息。姓名、职称、简介、头像、作品集（JSON数组）、排序

### 3.6 兑换上下文 (EXC) — 已规划未实现
- **ExchangeItem**: 兑换商品。名称、所需积分、库存、描述、图片
- **ExchangeRecord**: 兑换记录。关联用户和商品，有领取码和状态

### 3.7 管理上下文 (ADM)
- **AdminWhitelist**: 管理员白名单，通过 OpenID 鉴权
- 管理功能：画室配置、教师CRUD、活动CRUD、奖品CRUD

## 4. 领域边界

### 4.1 依赖矩阵

| 消费方 → 提供方 | USR | ACT | LOT | SHR | STU | TCH | EXC | ADM |
|-----------------|-----|-----|-----|-----|-----|-----|-----|-----|
| USR             |  -  |     |     |     |     |     |     |     |
| ACT             |     |  -  |     |     |     |     |     |     |
| LOT             |  ✓  |  ✓  |  -  |     |     |     |     |     |
| SHR             |  ✓  |  ✓  |  ✓  |  -  |     |     |     |     |
| EXC             |  ✓  |     |     |     |     |     |  -  |     |
| ADM             |  ✓  |  ✓  |  ✓  |     |  ✓  |  ✓  |     |  -  |

### 4.2 关键依赖关系
- **LOT → USR**: 抽奖时扣减用户抽奖次数，积分奖品自动发放到用户余额
- **LOT → ACT**: 抽奖关联活动，受活动最大抽奖次数限制
- **SHR → USR/ACT/LOT**: 分享确认后给分享者增加抽奖机会（写 USR 的 lottery_chances）
- **ADM → 全部**: 管理后台对所有业务实体执行 CRUD

### 4.3 边界规则
- 用户积分只通过两个途径变更：抽奖中积分奖品（LOT写入）、积分兑换（EXC扣减）
- 抽奖机会只通过三个途径增加：分享确认（SHR）、活动首次访问（ACT）、管理员手动（ADM）
- 实物奖品核销只在管理端进行

## 5. 页面结构

### 5.1 用户端（Tab Bar 导航）
| 页面 | 路径 | 功能 |
|------|------|------|
| 首页 | `/pages/index/` | 画室介绍、活动列表、快捷入口 |
| 抽奖 | `/pages/lottery/` | 转盘抽奖界面、抽奖记录 |
| 我的 | `/pages/my/` | 个人信息、积分、抽奖记录 |
| 活动详情 | `/pages/activity/` | 活动信息、分享操作 |
| 分享助力 | `/pages/share/` | 分享确认着陆页 |
| 教师列表 | `/pages/teachers/` | 师资展示 |

### 5.2 管理端
| 页面 | 路径 | 功能 |
|------|------|------|
| 管理首页 | `/pages/admin/index/` | 管理入口 |
| 画室配置 | `/pages/admin/config/` | 基础信息编辑 |
| 教师管理 | `/pages/admin/teachers/` | 教师CRUD |
| 活动管理 | `/pages/admin/activities/` | 活动CRUD |
| 奖品管理 | `/pages/admin/prizes/` | 奖品配置 |

## 6. API 端点概览

### 6.1 公开端点（无需认证）
- `POST /api/user/login` — 微信登录
- `GET /api/activity/list` — 活动列表
- `GET /api/activity/{id}` — 活动详情
- `GET /api/lottery/prizes` — 奖品池
- `GET /api/teacher/list` — 教师列表
- `GET /api/studio/config` — 画室配置

### 6.2 认证端点（需 JWT）
- `GET /api/user/info` — 用户信息
- `PUT /api/user/update` — 更新资料
- `POST /api/activity/visit` — 记录访问
- `POST /api/lottery/draw` — 执行抽奖
- `GET /api/lottery/status` — 抽奖状态
- `GET /api/lottery/records` — 抽奖记录
- `POST /api/share/create` — 发起分享
- `POST /api/share/confirm` — 确认分享
- `GET /api/share/status` — 分享状态

### 6.3 管理端点（需 JWT + 管理员白名单）
- `GET/POST /api/admin/config` — 配置读写
- `GET/POST/DELETE /api/admin/teacher[s]` — 教师管理
- `GET/POST/DELETE /api/admin/activit[ies|y]` — 活动管理
- `GET/POST/DELETE /api/admin/prize[s]` — 奖品管理

## 7. 部署架构

```
用户微信 → Nginx(443/80) → Spring Boot(8080) → MySQL(3306)
                                                     ↑
                                               Docker Compose
```

- Nginx 做 SSL 终止和反向代理
- 后端只监听 127.0.0.1:8080
- MySQL 数据持久化到 Docker Volume
- 环境变量管理敏感配置（DB密码、微信密钥、JWT密钥）
