# 错误策略 - CRS (课程管理) & PST (分享海报)

## 1. 现有错误处理体系

基于代码分析，当前系统采用三层错误处理机制：

### 1.1 统一响应格式

**代码依据**: `backend/src/main/java/com/shuran/art/dto/Result.java`

```java
Result.success(data)   → {code: 0,  msg: "success", data: {...}}
Result.success()       → {code: 0,  msg: "success", data: null}
Result.error(msg)      → {code: -1, msg: "错误描述", data: null}
Result.error(code,msg) → {code: N,  msg: "错误描述", data: null}
```

所有业务响应均使用 HTTP 200，通过 `code` 字段区分成功/失败。

### 1.2 拦截器层错误

**代码依据**: `AuthInterceptor.java`、`AdminInterceptor.java`

| 拦截器 | HTTP 状态码 | 响应体 | 触发条件 |
|--------|------------|--------|---------|
| AuthInterceptor | 401 | `{code:401, msg:"未登录或登录已过期"}` | JWT Token 缺失或无效 |
| AdminInterceptor | 401 | `{code:401, msg:"未登录或登录已过期"}` | JWT Token 缺失或无效 |
| AdminInterceptor | 403 | `{code:403, msg:"无管理员权限"}` | 用户不在管理员白名单 |

注意：拦截器直接写 response，不经过 Result 包装器，响应体中无 `data` 字段。

### 1.3 全局异常处理

**代码依据**: `backend/src/main/java/com/shuran/art/config/GlobalExceptionHandler.java`

| 异常类型 | 处理方式 | 响应 |
|---------|---------|------|
| `RuntimeException` | 日志记录 + 返回异常消息 | `Result.error(e.getMessage())` |
| `Exception` | 日志记录 + 返回通用消息 | `Result.error("系统异常，请稍后重试")` |

**现有模式**: 业务代码通过 `throw new RuntimeException("错误消息")` 抛出业务异常（参见 `LotteryService.draw()`），由 GlobalExceptionHandler 统一捕获并包装为 `Result.error()`。

---

## 2. CRS 领域错误策略

### 2.1 公开端点错误

CRS 公开端点遵循现有系统的宽容策略：查询不到数据时返回成功响应 + 空数据。

| 端点 | 场景 | 响应 | 设计依据 |
|------|------|------|---------|
| `GET /api/course/list` | 无上架课程 | `Result.success([])` — 空数组 | 参照 `TeacherController.getTeachers()` 模式 |
| `GET /api/course/list?category=X` | 该类别无课程 | `Result.success([])` — 空数组 | 前端显示"该类别暂无课程"（边界场景 B5） |
| `GET /api/course/{id}` | 课程不存在 | `Result.success(null)` — data 为 null | 参照 `ActivityController.getActivity()` 模式，前端判断 data |
| `GET /api/course/{id}` | 课程已下架 | `Result.success(course)` — 仍返回数据 | 管理端可能需要查看已下架课程详情 |

**不单独处理的场景**:
- ID 格式非法（非数字）：Spring 框架自动返回 400 Bad Request
- 数据库连接异常：GlobalExceptionHandler 捕获为"系统异常，请稍后重试"

### 2.2 管理端点错误

| 端点 | 场景 | 响应 | 设计依据 |
|------|------|------|---------|
| `GET /api/admin/courses` | 未登录 | HTTP 401 `{code:401, msg:"未登录或登录已过期"}` | AuthInterceptor |
| `GET /api/admin/courses` | 非管理员 | HTTP 403 `{code:403, msg:"无管理员权限"}` | AdminInterceptor |
| `POST /api/admin/course` | 必填字段缺失 | `Result.error("课程名称不能为空")` | Service 层校验后抛 RuntimeException |
| `POST /api/admin/course` | 保存成功 | `Result.success()` | 参照 `AdminController.saveTeacher()` |
| `DELETE /api/admin/course/{id}` | 课程不存在 | `Result.success()` — 静默成功 | 参照现有 `deleteTeacher` 模式，幂等处理 |
| `PUT /api/admin/course/{id}/status` | 课程不存在 | `Result.error("课程不存在")` | 状态切换需要明确反馈 |
| `PUT /api/admin/course/{id}/status` | status 值非法 | `Result.error("状态值无效")` | 仅接受 0 或 1 |

### 2.3 前端错误处理（CRS）

| 场景 | 前端行为 | 需求依据 |
|------|---------|---------|
| 课程列表为空 | 显示"暂无课程信息"空状态 | AC-CRS-001 |
| 筛选类别无结果 | 显示"该类别暂无课程"空状态 | 边界场景 B5 |
| 课程封面图加载失败 | 显示默认占位图 | 边界场景 B3 |
| 课程描述为空 | 详情页描述区域隐藏 | 边界场景 B2 |
| 网络请求失败 | 微信小程序通用网络错误提示 | 通用行为 |

---

## 3. PST 领域错误策略

### 3.1 后端错误（小程序码接口）

| 场景 | 错误来源 | 响应 | 需求依据 |
|------|---------|------|---------|
| 未登录 | AuthInterceptor | HTTP 401 `{code:401, msg:"未登录或登录已过期"}` | AC-4.4 |
| 活动不存在 | ShareService 校验 | HTTP 200 `Result.error("活动不存在")` | AC-4.4, BS-017 |
| access_token 获取失败 | 微信 API 网络异常 | 内部重试 1 次；仍失败则返回 `Result.error("小程序码生成失败，请稍后重试")` | 决策 5, BS-009 |
| wxacode API 调用失败 | 微信 API 返回错误 | 内部重试 1 次；仍失败则返回 `Result.error("小程序码生成失败，请稍后重试")` | BS-009 |
| wxacode API 返回错误码 | 微信返回 errcode != 0 | 日志记录 errcode 和 errmsg，返回 `Result.error("小程序码生成失败，请稍后重试")` | BS-009 |
| 缓存满（500 条） | LRU 淘汰 | 透明处理，淘汰最久未用条目，不返回错误 | REQ-PST-023, BS-018 |
| activityId 参数缺失 | Spring 参数绑定 | HTTP 400 Bad Request（Spring 框架默认行为） | — |

**重试策略**:
- 仅对微信 API 网络异常进行重试，不对业务错误（如 access_token 无效）进行重试
- 重试间隔：立即重试（无延迟），最多 1 次
- access_token 失效时先刷新 token 再重试 wxacode 调用

### 3.2 前端错误（海报生成流程）

| 场景 | 前端行为 | 需求依据 |
|------|---------|---------|
| 小程序码接口请求失败 | 关闭加载态，Toast "小程序码获取失败，请检查网络后重试"（3s），恢复按钮可点击 | BS-007 |
| 活动封面图下载超时(5s) | 使用本地默认占位图 `/images/default-activity.png` 继续绘制 | BS-008 |
| shareImage 和 coverImg 均为空 | 使用本地默认占位图继续绘制 | BS-001 |
| 画室名称未配置 | 使用默认值"舒然画室" | BS-002 |
| 活动标题超长(>40 汉字) | 截断并追加"..." | BS-004 |
| Canvas 绘制异常 | 关闭加载态，Toast "海报生成失败，请重试"（3s），恢复按钮可点击 | REQ-PST-014 |
| 用户未登录时点击生成海报 | 自动触发微信静默登录，成功后继续；失败显示 Toast "登录失败，请重试" | BS-011 |

### 3.3 前端错误（海报保存与分享）

| 场景 | 前端行为 | 需求依据 |
|------|---------|---------|
| 保存相册 - 权限被拒绝 | 显示模态弹窗"请在设置中开启相册权限" + "去设置"/"取消"按钮 | REQ-PST-007, BS-010 |
| 保存相册 - 存储空间不足 | Toast "保存失败，请检查手机存储空间"（3s） | 需求文档 6.2 |
| 分享给好友 - 用户取消 | 不显示错误提示（用户主动行为） | 需求文档 6.2 |
| 分享给好友 - 接口异常 | Toast "分享失败，请重试"（3s） | 需求文档 6.2 |

### 3.4 扫码着陆错误

| 场景 | 前端行为 | 需求依据 |
|------|---------|---------|
| scene 参数解析失败 | 跳转首页（降级） | 通用防御 |
| actId 对应活动不存在 | 显示"活动不存在或已下架"，引导返回首页 | BS-017 |
| scene 中无 shareFrom | 正常显示活动详情，不传递分享追踪参数 | 容错设计 |

---

## 4. 错误码体系

本系统沿用现有的简单错误码方案，不引入自定义错误码体系：

| code | 含义 | 使用场景 |
|------|------|---------|
| `0` | 成功 | 所有正常响应，包括查询结果为空 |
| `-1` | 业务失败 | 通过 `Result.error(msg)` 返回，msg 字段携带具体错误描述 |
| `401` | 未认证 | 仅在 AuthInterceptor/AdminInterceptor 中使用，直接写 HTTP 401 |
| `403` | 无权限 | 仅在 AdminInterceptor 中使用，直接写 HTTP 403 |

**设计依据**: 现有系统所有业务错误均使用 `code=-1` + 不同的 `msg` 描述来区分错误类型。项目规模较小（单体应用），无需引入多级错误码体系。保持与 `LotteryService`、`ShareService` 等已有代码的一致性。

---

## 5. 日志策略

沿用现有 GlobalExceptionHandler 的日志模式：

| 级别 | 场景 | 内容 |
|------|------|------|
| ERROR | RuntimeException 被捕获 | 异常堆栈（`log.error("运行时异常: ", e)`） |
| ERROR | 未预期 Exception 被捕获 | 异常堆栈（`log.error("系统异常: ", e)`） |
| WARN | 微信 API 调用失败（PST） | 请求参数 + 微信返回的 errcode/errmsg |
| WARN | access_token 获取失败（PST） | 请求参数 + HTTP 状态码 |
| INFO | 小程序码缓存命中/未命中（PST） | userId + activityId + 是否命中 |

CRS 领域为简单 CRUD，无额外日志需求，复用 MyBatis Plus 的 SQL 日志（`application.yml` 中已开启 `StdOutImpl`）。

---

## 6. 需求覆盖追溯

| 需求 / 边界场景 | 本文档对应章节 |
|----------------|--------------|
| EARS-CRS-001 ~ 005 | 2.1, 2.2 |
| AC-CRS-001 ~ 004 | 2.1, 2.2, 2.3 |
| B1 ~ B7 (CRS) | 2.1, 2.2, 2.3 |
| REQ-PST-001 ~ 004 | 3.2 (前端错误) |
| REQ-PST-005 ~ 008 | 3.3 (保存与分享) |
| REQ-PST-009 ~ 011 | 3.1 (后端错误) |
| REQ-PST-012 ~ 013 | 3.4 (扫码着陆) |
| REQ-PST-014 | 3.2 (加载态/防重复) |
| REQ-PST-020 ~ 023 | 3.1 (性能相关错误) |
| BS-001 ~ 018 (PST) | 3.1, 3.2, 3.3, 3.4 |
