# 学员作品展示墙 - 设计文档

## 概述
家长每月上传一张孩子作品，管理员审核后展示在作品墙。每个孩子有时间线页面，最新作品可分享到朋友圈集赞。最多保留24张（24个月）。

## 数据模型

### student_child 表（孩子）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| user_id | BIGINT | 家长用户ID |
| name | VARCHAR(32) | 孩子姓名 |
| avatar | VARCHAR(512) | 头像URL（可选）|
| created_at | DATETIME | 创建时间 |

### student_work 表（作品）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增 |
| child_id | BIGINT | 关联孩子 |
| user_id | BIGINT | 上传者（家长）|
| image_url | VARCHAR(512) | 作品图片 |
| description | VARCHAR(256) | 描述（可选）|
| status | VARCHAR(16) | pending/approved/rejected |
| created_at | DATETIME | 上传时间 |

### studio_config 新增配置
- `work_upload_interval_days` — 上传间隔天数（默认30）
- `work_share_text` — 分享默认文案（默认"快来看我在书染美术的作品～"）

## 页面设计

### 用户端
1. **作品墙** `pages/works/works` — 瀑布流展示已审核作品
2. **时间线** `pages/works/timeline` — 孩子作品按月排列，最新可分享
3. **上传** `pages/works/upload` — 选择孩子+上传图片+描述

### 管理后台
4. **学生作品管理** `pages/admin/works/works` — 审核+配置分享文案+上传间隔

### 入口
- 首页 feature grid 加"作品墙"
- 管理后台加"学生作品"菜单

## 业务规则
- 每个孩子每 N 天（可配置，默认30）上传一张
- 最多保留24张/孩子，超过自动归档最早的
- 只有最新一张已审核作品可分享
- 分享文案 = 用户自定义 + 默认文案后缀
- ��友圈集赞走线下（截图给老师领奖）
