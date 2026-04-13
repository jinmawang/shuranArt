---
review_round: 1
date: "2026-03-26"
reviewer: "challenger"
status: pass
blocking: 0
warning: 3
info: 5
---

# Challenger 审查报告 - Round 1

## 审查对象
- REQ-EXC-001: 积分兑换系统
- REQ-CRS-001: 课程介绍系统
- REQ-PST-001: 分享海报生成
- impact.md: 影响分析报告

## 审查结论: PASS

三份需求文档覆盖度良好，影响分析准确，无阻塞性问题。存在 3 个 warning 和 5 个 info 建议。

---

## 1. 覆盖性审查

### 1.1 REQ-EXC-001 积分兑换系统 — PASS
- [x] 用户故事完整（浏览、兑换、记录、核销、管理 5 个角色场景）
- [x] EARS 格式规范，条件-行为描述清晰
- [x] Gherkin 验收标准覆盖正常流、异常流、边界场景
- [x] 并发场景已考虑（库存竞争）
- [x] 兑换码格式和有效期已定义

**Warning W1**: 缺少"用户积分变动记录"的需求。当前只能通过 exchange_record 看到兑换扣减，但用户无法查看完整的积分收支明细（抽奖获得 + 兑换扣减）。建议在后续迭代中补充积分流水功能。

### 1.2 REQ-CRS-001 课程介绍系统 — PASS
- [x] 用户故事覆盖浏览、详情、筛选、管理 4 个场景
- [x] 分类筛选需求清晰
- [x] 管理后台 CRUD 参照现有 Teacher 模式

**Info I1**: 课程分类（category）的取值范围未定义。建议在实现时确定预设类别（如素描、水彩、油画、国画等），可在管理后台配置或代码中硬编码。

**Info I2**: 未明确课程与教师的关联关系。如果需要在课程详情中展示授课教师，需在 course 表增加 teacher_id 字段。当前需求中未提及此关联，可作为后续增强。

### 1.3 REQ-PST-001 分享海报生成 — PASS
- [x] 前端 Canvas 方案可行，技术路径清晰
- [x] 图片降级策略完整（share_image → cover_img → 默认图）
- [x] 权限处理已覆盖（相册权限请求/拒绝）
- [x] 小程序码参数定义明确

**Warning W2**: 小程序码生成接口需要微信 access_token，当前系统仅在用户登录时调用微信 API（jscode2session），未实现 access_token 管理。需要新增 access_token 获取和缓存机制（2小时有效期，需定时刷新）。影响分析中已标注此点。

**Info I3**: 海报尺寸未精确定义。建议固定为 750x1334px（iPhone 6/7/8 逻辑分辨率），确保在各机型上显示一致。

---

## 2. 可行性审查

### 2.1 技术可行性 — PASS
- [x] EXC: 数据库表已存在，实体类和 Mapper 已有，仅需 Controller + Service + 前端页面
- [x] CRS: 参照 Teacher 模式实现，开发模式成熟
- [x] PST: Canvas 2D API 在基础库 2.9.0+ 广泛支持，微信小程序目标用户覆盖率 >99%

### 2.2 兼容性 — PASS
- [x] 三个功能均为纯新增，零破坏性变更
- [x] 公开端点排除清单（WebMvcConfig）需更新但不影响已有端点
- [x] 首页布局需统一规划但为 UI 层面调整

### 2.3 安全性 — PASS
**Warning W3**: EXC 积分兑换操作需确保事务原子性。当前 LotteryService 中积分增加使用 MyBatis Plus 的 update 操作，建议 EXC 兑换扣减也使用 SQL 层面的原子操作（`UPDATE user SET points = points - ? WHERE id = ? AND points >= ?`），通过 affected rows 判断是否成功，避免 read-then-write 的竞态条件。

### 2.4 性能 — PASS
- [x] EXC 商品列表量小（预期 <50 项），无分页需求
- [x] CRS 课程列表量小（预期 <30 项），无分页需求
- [x] PST Canvas 绘制为前端操作，不增加服务器负载

---

## 3. 拆分建议

### 3.1 需求粒度 — 合适
三个需求文档粒度适中，各自独立完整。

### 3.2 实施顺序建议
影响分析中的推荐顺序合理：
1. CRS（完全独立，零依赖）+ PST（依赖已有数据，需新增 access_token 管理）并行
2. EXC（涉及积分事务，复杂度最高）
3. 统一首页和管理后台布局调整

**Info I4**: CRS 和 EXC 都需要在管理后台 admin/index 页面新增入口，建议在最后统一调整管理首页布局。

**Info I5**: 建议在实施 PST 之前，先确认微信小程序的 wxacode.getUnlimited 接口调用权限（需已认证的小程序）。个人主体小程序可能有此接口限制。

---

## 问题汇总

| 级别 | 编号 | 领域 | 问题 | 建议 |
|------|------|------|------|------|
| Warning | W1 | EXC | 缺少积分流水/收支明细需求 | 后续迭代补充 |
| Warning | W2 | PST | 需新增 access_token 管理机制 | 实施时一并开发 |
| Warning | W3 | EXC | 积分扣减需原子 SQL 操作 | 实施时使用 SQL 条件更新 |
| Info | I1 | CRS | 课程分类取值范围未定义 | 实施时确定预设类别 |
| Info | I2 | CRS | 课程与教师关联关系未明确 | 可作后续增强 |
| Info | I3 | PST | 海报尺寸未精确定义 | 建议 750x1334px |
| Info | I4 | ADM | 管理首页布局需统一调整 | 最后统一处理 |
| Info | I5 | PST | wxacode 接口需确认小程序认证状态 | 实施前验证 |
