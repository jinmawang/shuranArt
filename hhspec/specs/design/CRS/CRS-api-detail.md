# CRS API 详细设计

> **L2 详设** -- 基于 L1 架构产出 `CRS.openapi.yml` 和现有代码模式细化。

## 1. 端点总览

| # | 方法 | 路径 | 认证 | 需求来源 | 代码参照 |
|---|------|------|------|---------|---------|
| EP-01 | GET | `/api/course/list` | 无 | EARS-CRS-001, EARS-CRS-002, EARS-CRS-004 | TeacherController.getTeachers() |
| EP-02 | GET | `/api/course/{id}` | 无 | EARS-CRS-003 | ActivityController.getActivity() |
| EP-03 | GET | `/api/admin/courses` | JWT+Admin | EARS-CRS-005 | AdminController.getTeachers() |
| EP-04 | POST | `/api/admin/course` | JWT+Admin | EARS-CRS-005 | AdminController.saveTeacher() |
| EP-05 | DELETE | `/api/admin/course/{id}` | JWT+Admin | EARS-CRS-005 | AdminController.deleteTeacher() |
| EP-06 | PUT | `/api/admin/course/{id}/status` | JWT+Admin | EARS-CRS-005 | 新增（Teacher 无此端点） |

---

## 2. EP-01: GET /api/course/list -- 获取课程列表

### 2.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败行为 |
|------|------|------|------|------|---------|---------|
| V-001 | category | query | String | 否 | 无特殊验证；为空/null/不传时忽略 | 不适用 |

```
VALIDATE(request):
  category = request.queryParam("category")
  // category 为可选参数，无需验证
  // 空字符串视为"不传"，等同于查全部
  IF category == "" THEN category = null
```

### 2.2 业务逻辑

```
STEP-01: 构建查询条件
  queryWrapper = new LambdaQueryWrapper<Course>()
  queryWrapper.eq(Course::getStatus, 1)           // 仅上架课程
  queryWrapper.orderByAsc(Course::getSortOrder)    // 按 sort_order 升序

STEP-02: 应用分类筛选（条件）
  IF category != null AND category.isNotBlank() THEN
    queryWrapper.eq(Course::getCategory, category)
  END IF

STEP-03: 执行查询
  courses = courseMapper.selectList(queryWrapper)

STEP-04: 返回结果
  RETURN Result.success(courses)
  // courses 为空时返回空数组 []，前端显示"暂无课程信息"
```

**设计说明**：
- 参照 `TeacherController.getTeachers()` 模式，Controller 直接注入 CourseMapper，无 Service 层中转（简单查询场景）。
- `.eq(Course::getStatus, 1)` 确保仅返回上架课程，与 Teacher 列表 `.eq(Teacher::getStatus, 1)` 一致。
- 分类筛选通过 MyBatis Plus 的条件构造动态追加 WHERE 子句。

### 2.3 出参构造

```
成功响应（有数据）:
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "素描基础班",
      "category": "素描",
      "description": "<p>...</p>",
      "price": 2000,
      "duration": "3个月",
      "suitableFor": "零基础学员",
      "coverImg": "https://tianma.chat/uploads/course-sketch.jpg",
      "sortOrder": 1,
      "status": 1,
      "createdAt": "2026-03-01T10:00:00"
    }
  ]
}

成功响应（无数据）:
{
  "code": 0,
  "msg": "success",
  "data": []
}
```

### 2.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 查询成功（含空结果） | 200 | 0 | "success" | 正常流程 |
| 数据库异常 | 200 | -1 | "系统异常，请稍后重试" | GlobalExceptionHandler 捕获 |

---

## 3. EP-02: GET /api/course/{id} -- 获取课程详情

### 3.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败行为 |
|------|------|------|------|------|---------|---------|
| V-002 | id | path | Long | 是 | 正整数，Spring 自动绑定 | 非数字时 Spring 返回 400 |

```
VALIDATE(request):
  id = request.pathVariable("id")  // Long 类型，Spring 自动转换
  // 非法格式（如 "abc"）由 Spring 框架拦截，返回 400 Bad Request
```

### 3.2 业务逻辑

```
STEP-01: 根据 ID 查询课程
  course = courseMapper.selectById(id)

STEP-02: 返回结果
  RETURN Result.success(course)
  // course 为 null 时 data=null，前端判断处理
  // 即使课程已下架（status=0）也返回数据（管理端可能需要查看）
```

**设计说明**：
- 参照 `ActivityController.getActivity()` 模式：`selectById` + `Result.success(entity)`，不存在时 data=null。
- 不额外过滤 status，已下架的课程仍可通过 ID 直接访问（与 Activity 行为一致）。

### 3.3 出参构造

```
成功响应（课程存在）:
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": 1,
    "name": "素描基础班",
    "category": "素描",
    "description": "<p>零基础入门素描课程，从握笔姿势开始...</p>",
    "price": 2000,
    "duration": "3个月",
    "suitableFor": "零基础学员",
    "coverImg": "https://tianma.chat/uploads/course-sketch.jpg",
    "sortOrder": 1,
    "status": 1,
    "createdAt": "2026-03-01T10:00:00"
  }
}

成功响应（课程不存在）:
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

### 3.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 查询成功（含 null） | 200 | 0 | "success" | 正常流程 |
| ID 格式非法 | 400 | -- | Spring 默认错误 | path variable 非数字 |
| 数据库异常 | 200 | -1 | "系统异常，请稍后重试" | GlobalExceptionHandler 捕获 |

---

## 4. EP-03: GET /api/admin/courses -- 管理员获取全部课程

### 4.1 入参验证

```
VALIDATE(request):
  // 无业务参数
  // 认证由 AuthInterceptor 处理（JWT 验证）
  // 权限由 AdminInterceptor 处理（管理员白名单验证）
```

### 4.2 业务逻辑

```
STEP-01: 查询所有课程（含已下架）
  courses = adminService.getCourses()
  // AdminService.getCourses():
  //   RETURN courseMapper.selectList(
  //     new LambdaQueryWrapper<Course>()
  //       .orderByAsc(Course::getSortOrder)
  //   )

STEP-02: 返回结果
  RETURN Result.success(courses)
```

**设计说明**：
- 参照 `AdminController.getTeachers()` + `AdminService.getTeachers()` 模式。
- 管理端不过滤 status，返回全部课程（含下架），与 Teacher 管理行为一致。
- 排序使用 sortOrder 升序。

### 4.3 出参构造

与 EP-01 相同格式，但 data 中包含 status=0（已下架）的课程。

### 4.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 查询成功 | 200 | 0 | "success" | 正常流程 |
| 未登录 | 401 | 401 | "未登录或登录已过期" | AuthInterceptor 拦截 |
| 非管理员 | 403 | 403 | "无管理员权限" | AdminInterceptor 拦截 |

---

## 5. EP-04: POST /api/admin/course -- 新增或编辑课程

### 5.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败消息 |
|------|------|------|------|------|---------|---------|
| V-003 | id | body | Long | 否 | null=新增，有值=编辑 | -- |
| V-004 | name | body | String | 是 | 非空，maxLength=64 | "课程名称不能为空" |
| V-005 | category | body | String | 是 | 非空，maxLength=32 | "课程类别不能为空" |
| V-006 | price | body | Integer | 是 | >= 0 | "课程价格不能为负数" |
| V-007 | duration | body | String | 是 | 非空，maxLength=32 | "课程时长不能为空" |
| V-008 | suitableFor | body | String | 是 | 非空，maxLength=64 | "适合人群不能为空" |
| V-009 | coverImg | body | String | 否 | maxLength=512 | -- |
| V-010 | description | body | String | 否 | 无长度限制（TEXT） | -- |
| V-011 | sortOrder | body | Integer | 否 | 默认 0 | -- |
| V-012 | status | body | Integer | 否 | 0 或 1，新增默认 1 | "状态值无效" |

```
VALIDATE(course):
  IF course.name IS NULL OR course.name.isBlank() THEN
    THROW RuntimeException("课程名称不能为空")
  END IF

  IF course.category IS NULL OR course.category.isBlank() THEN
    THROW RuntimeException("课程类别不能为空")
  END IF

  IF course.price IS NULL OR course.price < 0 THEN
    THROW RuntimeException("课程价格不能为负数")
  END IF

  IF course.duration IS NULL OR course.duration.isBlank() THEN
    THROW RuntimeException("课程时长不能为空")
  END IF

  IF course.suitableFor IS NULL OR course.suitableFor.isBlank() THEN
    THROW RuntimeException("适合人群不能为空")
  END IF

  IF course.status != null AND course.status NOT IN (0, 1) THEN
    THROW RuntimeException("状态值无效")
  END IF

  // 设置默认值
  IF course.sortOrder IS NULL THEN course.sortOrder = 0
  IF course.status IS NULL THEN course.status = 1  // 新增时默认上架
```

### 5.2 业务逻辑

```
STEP-01: 验证输入（见 5.1）

STEP-02: 判断新增/编辑
  IF course.id IS NULL THEN
    // 新增
    STEP-03: courseMapper.insert(course)
  ELSE
    // 编辑
    STEP-03: courseMapper.updateById(course)
  END IF

STEP-04: 返回成功
  RETURN Result.success()
```

**设计说明**：
- 完全参照 `AdminService.saveTeacher()` 的 `id == null ? insert : updateById` 模式。
- 验证逻辑在 AdminService 层实现（通过 `throw new RuntimeException`），GlobalExceptionHandler 统一捕获。
- 与现有模式一致，验证放在 Service 层而非 Controller 层。

### 5.3 出参构造

```
成功响应:
{
  "code": 0,
  "msg": "success",
  "data": null
}

失败响应（字段校验）:
{
  "code": -1,
  "msg": "课程名称不能为空",
  "data": null
}
```

### 5.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 保存成功 | 200 | 0 | "success" | 新增或编辑成功 |
| 必填字段缺失 | 200 | -1 | "课程名称不能为空" 等 | Service 层校验 |
| 状态值非法 | 200 | -1 | "状态值无效" | status 非 0/1 |
| 未登录 | 401 | 401 | "未登录或登录已过期" | AuthInterceptor |
| 非管理员 | 403 | 403 | "无管理员权限" | AdminInterceptor |

---

## 6. EP-05: DELETE /api/admin/course/{id} -- 删除课程

### 6.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败行为 |
|------|------|------|------|------|---------|---------|
| V-013 | id | path | Long | 是 | 正整数 | Spring 400 |

### 6.2 业务逻辑

```
STEP-01: 删除课程（物理删除）
  adminService.deleteCourse(id)
  // AdminService.deleteCourse(id):
  //   courseMapper.deleteById(id)

STEP-02: 返回成功
  RETURN Result.success()
  // 课程不存在时也返回成功（幂等处理，与 deleteTeacher 一致）
```

**设计说明**：
- 参照 `AdminController.deleteTeacher()` + `AdminService.deleteTeacher()` 模式。
- 物理删除，deleteById 对不存在的 ID 不会报错（MyBatis Plus 行为），保持幂等性。
- 无确认逻辑（确认弹窗在前端处理，对应边界场景 B6）。

### 6.3 出参构造

```
成功响应:
{
  "code": 0,
  "msg": "success",
  "data": null
}
```

### 6.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 删除成功（含不存在） | 200 | 0 | "success" | 幂等处理 |
| 未登录 | 401 | 401 | "未登录或登录已过期" | AuthInterceptor |
| 非管理员 | 403 | 403 | "无管理员权限" | AdminInterceptor |

---

## 7. EP-06: PUT /api/admin/course/{id}/status -- 课程上下架切换

### 7.1 入参验证

| 编号 | 参数 | 来源 | 类型 | 必填 | 验证规则 | 失败消息 |
|------|------|------|------|------|---------|---------|
| V-014 | id | path | Long | 是 | 正整数 | Spring 400 |
| V-015 | status | body | Integer | 是 | 值为 0 或 1 | "状态值无效" |

```
VALIDATE(request):
  id = request.pathVariable("id")
  statusBody = request.body  // JSON: {"status": 0|1}
  status = statusBody.get("status")

  IF status IS NULL THEN
    THROW RuntimeException("状态值不能为空")
  END IF

  IF status NOT IN (0, 1) THEN
    THROW RuntimeException("状态值无效")
  END IF
```

### 7.2 业务逻辑

```
STEP-01: 验证课程存在
  course = courseMapper.selectById(id)
  IF course IS NULL THEN
    THROW RuntimeException("课程不存在")
  END IF

STEP-02: 更新状态
  course.setStatus(status)
  courseMapper.updateById(course)

STEP-03: 返回成功
  RETURN Result.success()
```

**设计说明**：
- 这是 CRS 领域新增端点，Teacher 模式中不存在（Teacher 无上下架需求）。
- 与 delete 不同，状态切换需要确认课程存在（否则切换无意义），因此做存在性校验。
- 下架后（status=0），课程在用户端 `/api/course/list` 中不可见（WHERE status=1），但管理端 `/api/admin/courses` 仍可见。

### 7.3 出参构造

```
成功响应:
{
  "code": 0,
  "msg": "success",
  "data": null
}

失败响应（课程不存在）:
{
  "code": -1,
  "msg": "课程不存在",
  "data": null
}
```

### 7.4 错误码映射

| 场景 | HTTP | code | msg | 触发条件 |
|------|------|------|-----|---------|
| 状态更新成功 | 200 | 0 | "success" | 正常流程 |
| 课程不存在 | 200 | -1 | "课程不存在" | selectById 返回 null |
| 状态值无效 | 200 | -1 | "状态值无效" | status 非 0/1 |
| 未登录 | 401 | 401 | "未登录或登录已过期" | AuthInterceptor |
| 非管理员 | 403 | 403 | "无管理员权限" | AdminInterceptor |

---

## 8. 路由注册

### 8.1 WebMvcConfig excludePathPatterns 新增

公开端点需在 `WebMvcConfig` 中注册排除拦截：

```
新增排除路径:
  - "/api/course/list"
  - "/api/course/*"
```

**设计说明**：参照现有 `/api/teacher/list`、`/api/activity/*` 的排除模式。

### 8.2 AdminInterceptor 覆盖

管理端点 `/api/admin/course*` 已被现有 `addPathPatterns("/api/admin/**")` 覆盖，无需额外配置。

---

## 9. 需求追溯

| 需求编号 | 对应端点 | 覆盖说明 |
|---------|---------|---------|
| EARS-CRS-001 | EP-01 | 课程列表，status=1，sortOrder ASC |
| EARS-CRS-002 | EP-01 | category 参数筛选 |
| EARS-CRS-003 | EP-02 | 课程详情（ID 查询） |
| EARS-CRS-004 | EP-01 | 首页课程入口复用列表接口（前端取前 4 项） |
| EARS-CRS-005 | EP-03~EP-06 | 管理后台 CRUD + 上下架 |
| AC-CRS-001 | EP-01 | 课程列表展示 + 空列表 |
| AC-CRS-002 | EP-01 | 分类筛选 |
| AC-CRS-003 | EP-02 | 课程详情 |
| AC-CRS-004 | EP-04, EP-06 | 管理员添加/下架课程 |
| B1 | EP-04 | price=0 允许（前端显示"免费"） |
| B6 | EP-05 | 删除后前端刷新列表 |
