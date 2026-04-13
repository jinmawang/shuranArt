---
review_round: 1
date: "2026-03-26"
reviewer: "l1_challenger"
target: "L1.2 OpenAPI contracts (CRS + PST)"
status: pass
blocking: 0
warning: 1
info: 2
---

# L1.2 架构审查报告 - Round 1

## 审查结论: PASS

CRS 和 PST 的 OpenAPI 接口契约质量良好，与现有 API 风格高度一致，需求覆盖完整。1 个 warning 和 2 个 info 不阻塞。

---

## 1. 一致性审查 — PASS

### 1.1 URL 路径风格
- CRS 公开端点 `/api/course/list`、`/api/course/{id}` 与现有 `/api/teacher/list`、`/api/activity/{id}` 完全一致 ✓
- CRS 管理端点 `/api/admin/courses`、`/api/admin/course`、`/api/admin/course/{id}` 与现有 `/api/admin/teachers`、`/api/admin/teacher`、`/api/admin/teacher/{id}` 完全一致 ✓
- PST 端点 `/api/share/wxacode` 与现有 `/api/share/status`、`/api/share/create` 路径风格一致 ✓

### 1.2 响应格式
- 全部使用 `Result<T>` 包装器（code/msg/data），与现有 `Result.success()`、`Result.error()` 模式一致 ✓
- 成功 code=0，失败 code=-1，与现有模式一致 ✓

### 1.3 认证方式
- CRS 公开端点需要加入 WebMvcConfig excludePathPatterns，契约中已注明 `/api/course/list` 和 `/api/course/*` ✓
- PST wxacode 端点需要 JWT 认证（获取 userId），不在 excludePathPatterns 中 ✓
- 管理端点在 `/api/admin/**` 下，AdminInterceptor 自动拦截 ✓

### 1.4 save/update 模式
- CRS `POST /api/admin/course` 采用 id 有值=更新/无值=新增 模式，与现有 `saveTeacher` 完全一致 ✓

## 2. 完整性审查 — PASS

### 2.1 CRS 需求覆盖
| 需求 | 对应端点 | 覆盖 |
|------|---------|------|
| EARS-CRS-001 课程列表 | GET /api/course/list | ✓ |
| EARS-CRS-002 分类筛选 | GET /api/course/list?category= | ✓ |
| EARS-CRS-003 课程详情 | GET /api/course/{id} | ✓ |
| EARS-CRS-004 首页课程入口 | GET /api/course/list (前端取前4) | ✓ |
| EARS-CRS-005 管理CRUD | GET/POST/DELETE /api/admin/course* + PUT status | ✓ |

### 2.2 PST 需求覆盖（后端部分）
| 需求 | 对应端点 | 覆盖 |
|------|---------|------|
| REQ-PST-009 小程序码生成 | GET /api/share/wxacode | ✓ |
| REQ-PST-010 跳转页面 | page 参数在契约描述中 | ✓ |
| REQ-PST-011 24h 缓存 | 内部行为已说明 | ✓ |
| REQ-PST-022 性能要求 | 已标注 <3s / <200ms | ✓ |
| REQ-PST-023 LRU 缓存 | 内部行为已说明 | ✓ |

**注**：前端 Canvas 绘制（REQ-PST-001~008）、相册权限（REQ-PST-005~007）、扫码着陆（REQ-PST-012~013）为前端职责，不在后端 API 契约范围。

## 3. 可行性审查 — PASS

- CRS: 完全参照 Teacher CRUD 模式，现有代码可直接复制改造，零技术风险 ✓
- PST: 需新增 access_token 管理（决策 5 已确认），微信 wxacode.getUnlimited API 为标准接口 ✓
- 新增 `PUT /api/admin/course/{id}/status` 端点实现简单（单字段 UPDATE），合理 ✓

## 4. 演进性审查 — PASS

- CRS 列表未分页（与 Teacher/Activity 一致），课程量小（<30）足够，未来可扩展分页参数 ✓
- PST Base64 返回方式简单直接，未来可扩展为 URL 返回或流式返回 ✓
- 所有端点为纯新增，零 breaking change ✓

## 5. 规范符合性审查 — PASS

- OpenAPI 3.0.3 格式正确 ✓
- Schema 字段有 type、description、约束（maxLength/minimum/enum/nullable）✓
- 示例数据完整（成功/失败场景）✓
- 安全方案（bearerAuth）正确定义 ✓

---

## 问题汇总

| 级别 | 编号 | 问题 | 建议 |
|------|------|------|------|
| Warning | W1 | PST scene 参数格式：需求 REQ-PST-009 使用长格式 `shareFrom={userId}&actId={activityId}`，BS-013 建议短格式 `s={userId}&a={actId}`，OpenAPI 使用 `s={userId}&a={activityId}`。需统一为 `s={userId}&a={actId}` 短格式（BS-013 明确推荐） | L3 实现时 scene 参数统一使用短格式 `s={userId}&a={actId}`，前端解析也对应使用短 key |
| Info | I1 | CRS `PUT /api/admin/course/{id}/status` 是现有模式中的新增端点类型，现有 Teacher 无独立状态切换端点 | 保留此端点，语义更清晰。实现简单（单字段 UPDATE） |
| Info | I2 | PST wxacode 缓存 key 格式 `{userId}_{activityId}` 在 OpenAPI 描述中提及，但属于内部实现细节 | 无需修改，仅作文档参考 |
