# 画室小程序技术设计文档

## 概述

个人画室小程序，核心功能是活动推广和分享裂变抽奖。

**技术栈：**
- 前端：微信小程序原生开发
- 后端：Spring Boot + MyBatis + MySQL
- 部署：Docker Compose（Nginx + Spring Boot + MySQL）
- 服务器：腾讯云轻量 4核4G

---

## 项目结构

```
shuranArt/
├── frontend/                    # 微信小程序前端
│   ├── pages/
│   ├── components/
│   ├── api/
│   ├── utils/
│   ├── app.js
│   ├── app.json
│   └── app.wxss
│
├── backend/                     # Spring Boot 后端
│   ├── src/main/java/com/shuran/art/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── config/
│   │   └── util/
│   ├── src/main/resources/
│   ├── Dockerfile
│   └── pom.xml
│
├── docker-compose.yml
├── nginx/
│   └── nginx.conf
└── docs/
```

---

## API 设计

基础路径: `https://域名/api/v1`

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 用户 | /user/login | POST | 微信登录 |
| 用户 | /user/info | GET | 获取用户信息 |
| 画室 | /studio/info | GET | 画室介绍 |
| 老师 | /teacher/list | GET | 老师列表 |
| 活动 | /activity/list | GET | 活动列表 |
| 活动 | /activity/{id} | GET | 活动详情 |
| 分享 | /share/record | POST | 记录分享 |
| 抽奖 | /lottery/draw | POST | 执行抽奖 |
| 抽奖 | /lottery/prizes | GET | 奖品列表 |
| 抽奖 | /lottery/records | GET | 抽奖记录 |
| 积分 | /points/exchange/items | GET | 兑换商品 |
| 积分 | /points/exchange | POST | 积分兑换 |
| 管理 | /admin/check | GET | 验证管理员身份 |
| 管理 | /admin/studio/config | GET/POST | 画室配置 |
| 管理 | /admin/teachers | GET | 老师列表（管理） |
| 管理 | /admin/teacher | POST | 保存老师 |
| 管理 | /admin/teacher/{id} | DELETE | 删除老师 |
| 管理 | /admin/activities | GET | 活动列表（管理） |
| 管理 | /admin/activity | POST | 保存活动 |
| 管理 | /admin/activity/{id} | DELETE | 删除活动 |
| 管理 | /admin/prizes | GET | 奖品列表（管理） |
| 管理 | /admin/prize | POST | 保存奖品 |
| 管理 | /admin/prize/{id} | DELETE | 删除奖品 |

---

## 数据库设计

### user 用户表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| openid | VARCHAR(64) | 微信openid，唯一 |
| nick_name | VARCHAR(64) | 昵称 |
| avatar_url | VARCHAR(512) | 头像 |
| phone | VARCHAR(20) | 手机号 |
| points | INT | 积分 |
| lottery_chances | INT | 抽奖次数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### teacher 老师表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(32) | 姓名 |
| title | VARCHAR(32) | 职称 |
| intro | TEXT | 简介 |
| avatar | VARCHAR(512) | 头像 |
| works | JSON | 作品列表 |
| sort_order | INT | 排序 |

### course 课程表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 课程名 |
| description | TEXT | 描述 |
| price | DECIMAL(10,2) | 价格 |
| duration | VARCHAR(32) | 时长 |
| suitable_for | VARCHAR(128) | 适合人群 |
| cover_img | VARCHAR(512) | 封面图 |
| sort_order | INT | 排序 |

### activity 活动表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR(128) | 标题 |
| description | TEXT | 描述 |
| cover_img | VARCHAR(512) | 封面图 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| daily_share_limit | INT | 每日分享上限 |
| status | TINYINT | 状态 1进行中 0结束 |

### prize 奖品表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 奖品名 |
| type | VARCHAR(20) | 类型 points/experience/gift |
| value | INT | 数值 |
| probability | INT | 概率(%) |
| stock | INT | 库存，-1无限 |
| icon | VARCHAR(512) | 图标 |
| need_claim | TINYINT | 是否需线下领取 |
| status | TINYINT | 状态 |

### share_record 分享记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| sharer_id | BIGINT | 分享者ID |
| visitor_id | BIGINT | 访客ID |
| activity_id | BIGINT | 活动ID |
| lottery_granted | TINYINT | 是否已发抽奖 |
| created_at | DATETIME | 创建时间 |

唯一索引: (sharer_id, visitor_id, activity_id)

### lottery_record 抽奖记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| prize_id | BIGINT | 奖品ID |
| prize_name | VARCHAR(64) | 奖品名 |
| prize_type | VARCHAR(20) | 类型 |
| prize_value | INT | 数值 |
| status | VARCHAR(20) | pending/claimed/expired |
| claim_code | VARCHAR(16) | 领取码 |
| created_at | DATETIME | 创建时间 |
| claimed_at | DATETIME | 领取时间 |
| expire_at | DATETIME | 过期时间 |

### exchange_item 兑换商品表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(64) | 商品名 |
| points_cost | INT | 所需积分 |
| stock | INT | 库存 |
| description | VARCHAR(256) | 描述 |
| image | VARCHAR(512) | 图片 |
| need_claim | TINYINT | 是否需线下领取 |
| status | TINYINT | 状态 |

### exchange_record 兑换记录表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| item_id | BIGINT | 商品ID |
| item_name | VARCHAR(64) | 商品名 |
| points_cost | INT | 消耗积分 |
| claim_code | VARCHAR(16) | 领取码 |
| status | VARCHAR(20) | pending/claimed/expired |
| created_at | DATETIME | 创建时间 |
| claimed_at | DATETIME | 领取时间 |
| expire_at | DATETIME | 过期时间 |

---

## 前端页面

```
pages/
├── index/              # 首页（画室介绍 + 活动入口）
├── teacher/list/       # 老师介绍
├── activity/list/      # 活动列表
├── activity/detail/    # 活动详情（分享页）
├── lottery/index/      # 九宫格抽奖
├── user/index/         # 个人中心（奖品+活动记录）
└── admin/              # 后台管理（仅管理员可见）
    ├── index/          # 管理入口
    ├── studio/         # 画室配置
    ├── teacher/        # 老师管理
    ├── activity/       # 活动管理
    └── prize/          # 奖品管理
```

**TabBar:**
```
[ 首页 ]  [ 活动 ]  [ 我的 ]
```

---

## 部署架构

```
Docker Compose:
├── nginx (443/80) → 反向代理 + SSL
├── spring-boot (8080) → 业务API
└── mysql (3306) → 数据存储

数据卷:
├── mysql_data → 数据持久化
└── nginx_ssl → SSL证书
```

---

## 后台管理功能

### 管理员权限控制
- 基于微信 openid 白名单控制
- 管理员白名单存储在 `admin_whitelist` 表中
- 前端在用户中心检测管理员身份，显示/隐藏入口

### 管理功能模块

| 模块 | 功能 | 说明 |
|------|------|------|
| 画室配置 | 文字/图片/视频 | 配置首页展示的画室介绍 |
| 老师管理 | 增删改查 | 管理老师信息和排序 |
| 活动管理 | 增删改查 | 发布和编辑推广活动 |
| 奖品管理 | 增删改查 | 配置抽奖奖品池 |
| 分享文案 | 模板配置 | 自定义分享标题和描述 |

### 管理员相关表

#### admin_whitelist 管理员白名单表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| openid | VARCHAR(64) | 微信openid，唯一 |
| name | VARCHAR(32) | 管理员名称 |
| created_at | DATETIME | 创建时间 |

#### studio_config 画室配置表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| config_key | VARCHAR(64) | 配置键，唯一 |
| config_value | TEXT | 配置值 |
| config_type | VARCHAR(20) | 类型 text/json |
| updated_at | DATETIME | 更新时间 |

### 管理端 API

| 接口 | 方法 | 说明 |
|------|------|------|
| /admin/check | GET | 验证管理员身份 |
| /admin/studio/config | GET | 获取画室配置 |
| /admin/studio/config | POST | 更新画室配置 |
| /admin/teachers | GET | 获取老师列表 |
| /admin/teacher | POST | 保存老师 |
| /admin/teacher/{id} | DELETE | 删除老师 |
| /admin/activities | GET | 获取活动列表 |
| /admin/activity | POST | 保存活动 |
| /admin/activity/{id} | DELETE | 删除活动 |
| /admin/prizes | GET | 获取奖品列表 |
| /admin/prize | POST | 保存奖品 |
| /admin/prize/{id} | DELETE | 删除奖品 |

---

## 核心业务流程

### 分享抽奖流程
```
用户A分享活动 → 用户B点击进入 → 记录分享关系 →
检查今日上限 → 给用户A +1抽奖机会 →
用户A抽奖 → 按概率抽中奖品 →
积分自动发放 / 实物生成领取码
```

### 防刷规则
- 每人每天最多获得5次抽奖机会
- 同一访客对同一分享者同一活动只算1次
- 不能给自己助力
