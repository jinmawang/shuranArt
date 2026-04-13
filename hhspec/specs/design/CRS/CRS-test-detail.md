# CRS 测试设计

> **L2 详设** -- 基于 L0 验收标准（AC-CRS-001 ~ AC-CRS-004）和 L2 API/数据层设计产出。

## 1. 测试点清单

| 编号 | 测试层级 | 测试目标 | 优先级 | 关联需求 |
|------|---------|---------|--------|---------|
| TP-CRS-001 | 单元测试 | AdminService.saveCourse 新增逻辑 | P0 | EARS-CRS-005, AC-CRS-004 |
| TP-CRS-002 | 单元测试 | AdminService.saveCourse 编辑逻辑 | P0 | EARS-CRS-005 |
| TP-CRS-003 | 单元测试 | AdminService.saveCourse 字段校验 | P0 | EARS-CRS-005, V-004~V-012 |
| TP-CRS-004 | 单元测试 | AdminService.deleteCourse 幂等性 | P1 | EARS-CRS-005, B6 |
| TP-CRS-005 | 单元测试 | AdminService.updateCourseStatus 正常流程 | P0 | EARS-CRS-005, AC-CRS-004 |
| TP-CRS-006 | 单元测试 | AdminService.updateCourseStatus 课程不存在 | P1 | EARS-CRS-005 |
| TP-CRS-007 | 集成测试 | GET /api/course/list 课程列表（有数据） | P0 | EARS-CRS-001, AC-CRS-001 |
| TP-CRS-008 | 集成测试 | GET /api/course/list 空列表 | P1 | AC-CRS-001, B4 |
| TP-CRS-009 | 集成测试 | GET /api/course/list?category 分类筛选 | P0 | EARS-CRS-002, AC-CRS-002 |
| TP-CRS-010 | 集成测试 | GET /api/course/{id} 课程存在 | P0 | EARS-CRS-003, AC-CRS-003 |
| TP-CRS-011 | 集成测试 | GET /api/course/{id} 课程不存在 | P1 | EARS-CRS-003 |
| TP-CRS-012 | 集成测试 | GET /api/admin/courses 管理员获取全部 | P0 | EARS-CRS-005 |
| TP-CRS-013 | 集成测试 | POST /api/admin/course 新增课程 | P0 | EARS-CRS-005, AC-CRS-004 |
| TP-CRS-014 | 集成测试 | POST /api/admin/course 编辑课程 | P0 | EARS-CRS-005 |
| TP-CRS-015 | 集成测试 | DELETE /api/admin/course/{id} 删除课程 | P0 | EARS-CRS-005, B6 |
| TP-CRS-016 | 集成测试 | PUT /api/admin/course/{id}/status 上下架 | P0 | EARS-CRS-005, AC-CRS-004 |
| TP-CRS-017 | 集成测试 | 管理端点认证拦截（401/403） | P0 | EARS-CRS-005 |
| TP-CRS-018 | 集成测试 | GET /api/course/list 排序正确性 | P1 | EARS-CRS-001 |
| TP-CRS-019 | 集成测试 | POST /api/admin/course price=0 免费课程 | P2 | B1 |
| TP-CRS-020 | 集成测试 | GET /api/course/list?category=不存在的类别 | P2 | B5 |

---

## 2. 单元测试骨架

### 2.1 TP-CRS-001: AdminService.saveCourse 新增逻辑

```
TEST: "saveCourse - 新增课程 - id 为 null 时调用 insert"
  关联: EARS-CRS-005, AC-CRS-004（管理员添加课程）

  // Arrange
  course = new Course()
  course.setId(null)
  course.setName("国画入门班")
  course.setCategory("国画")
  course.setPrice(2500)
  course.setDuration("2个月")
  course.setSuitableFor("零基础")
  mock(courseMapper.insert(any(Course.class))).returns(1)

  // Act
  adminService.saveCourse(course)

  // Assert
  verify(courseMapper).insert(course)
  verify(courseMapper, never()).updateById(any())
```

### 2.2 TP-CRS-002: AdminService.saveCourse 编辑逻辑

```
TEST: "saveCourse - 编辑课程 - id 有值时调用 updateById"
  关联: EARS-CRS-005

  // Arrange
  course = new Course()
  course.setId(1L)
  course.setName("素描基础班（已更新）")
  course.setCategory("素描")
  course.setPrice(2200)
  course.setDuration("3个月")
  course.setSuitableFor("零基础学员")
  mock(courseMapper.updateById(any(Course.class))).returns(1)

  // Act
  adminService.saveCourse(course)

  // Assert
  verify(courseMapper).updateById(course)
  verify(courseMapper, never()).insert(any())
```

### 2.3 TP-CRS-003: AdminService.saveCourse 字段校验

```
TEST: "saveCourse - 名称为空 - 抛出 RuntimeException"
  关联: EARS-CRS-005, V-004

  // Arrange
  course = new Course()
  course.setName(null)
  course.setCategory("素描")
  course.setPrice(2000)
  course.setDuration("3个月")
  course.setSuitableFor("零基础")

  // Act & Assert
  assertThrows(RuntimeException.class, () -> adminService.saveCourse(course))
    .hasMessage("课程名称不能为空")

---

TEST: "saveCourse - 类别为空 - 抛出 RuntimeException"
  关联: EARS-CRS-005, V-005

  // Arrange
  course = new Course()
  course.setName("素描班")
  course.setCategory(null)
  course.setPrice(2000)
  course.setDuration("3个月")
  course.setSuitableFor("零基础")

  // Act & Assert
  assertThrows(RuntimeException.class, () -> adminService.saveCourse(course))
    .hasMessage("课程类别不能为空")

---

TEST: "saveCourse - 价格为负数 - 抛出 RuntimeException"
  关联: EARS-CRS-005, V-006

  // Arrange
  course = new Course()
  course.setName("素描班")
  course.setCategory("素描")
  course.setPrice(-100)
  course.setDuration("3个月")
  course.setSuitableFor("零基础")

  // Act & Assert
  assertThrows(RuntimeException.class, () -> adminService.saveCourse(course))
    .hasMessage("课程价格不能为负数")

---

TEST: "saveCourse - 状态值非法 - 抛出 RuntimeException"
  关联: EARS-CRS-005, V-012

  // Arrange
  course = new Course()
  course.setName("素描班")
  course.setCategory("素描")
  course.setPrice(2000)
  course.setDuration("3个月")
  course.setSuitableFor("零基础")
  course.setStatus(2)  // 非法值

  // Act & Assert
  assertThrows(RuntimeException.class, () -> adminService.saveCourse(course))
    .hasMessage("状态值无效")
```

### 2.4 TP-CRS-004: AdminService.deleteCourse 幂等性

```
TEST: "deleteCourse - 课程存在 - 正常删除"
  关联: EARS-CRS-005, B6

  // Arrange
  mock(courseMapper.deleteById(1L)).returns(1)

  // Act
  adminService.deleteCourse(1L)

  // Assert
  verify(courseMapper).deleteById(1L)

---

TEST: "deleteCourse - 课程不存在 - 不报错（幂等）"
  关联: EARS-CRS-005

  // Arrange
  mock(courseMapper.deleteById(999L)).returns(0)

  // Act (should not throw)
  adminService.deleteCourse(999L)

  // Assert
  verify(courseMapper).deleteById(999L)
```

### 2.5 TP-CRS-005: AdminService.updateCourseStatus 正常流程

```
TEST: "updateCourseStatus - 上架转下架 - 状态更新成功"
  关联: EARS-CRS-005, AC-CRS-004（管理员下架课程）

  // Arrange
  existingCourse = new Course()
  existingCourse.setId(1L)
  existingCourse.setStatus(1)
  mock(courseMapper.selectById(1L)).returns(existingCourse)
  mock(courseMapper.updateById(any())).returns(1)

  // Act
  adminService.updateCourseStatus(1L, 0)

  // Assert
  verify(courseMapper).updateById(argThat(c -> c.getStatus() == 0))

---

TEST: "updateCourseStatus - 下架转上架 - 状态更新成功"
  关联: EARS-CRS-005

  // Arrange
  existingCourse = new Course()
  existingCourse.setId(1L)
  existingCourse.setStatus(0)
  mock(courseMapper.selectById(1L)).returns(existingCourse)
  mock(courseMapper.updateById(any())).returns(1)

  // Act
  adminService.updateCourseStatus(1L, 1)

  // Assert
  verify(courseMapper).updateById(argThat(c -> c.getStatus() == 1))
```

### 2.6 TP-CRS-006: AdminService.updateCourseStatus 课程不存在

```
TEST: "updateCourseStatus - 课程不存在 - 抛出 RuntimeException"
  关联: EARS-CRS-005

  // Arrange
  mock(courseMapper.selectById(999L)).returns(null)

  // Act & Assert
  assertThrows(RuntimeException.class, () -> adminService.updateCourseStatus(999L, 0))
    .hasMessage("课程不存在")
```

---

## 3. 集成测试骨架

### 3.1 TP-CRS-007: 课程列表（有数据）

```
TEST: "GET /api/course/list - 返回上架课程列表并按 sortOrder 排序"
  关联: EARS-CRS-001, AC-CRS-001（查看课程列表）

  // Arrange
  // 数据库预置：3 个上架课程 + 1 个下架课程
  INSERT course(name="素描基础班", category="素描", price=2000, sort_order=1, status=1)
  INSERT course(name="水彩提高班", category="水彩", price=3000, sort_order=2, status=1)
  INSERT course(name="油画大师班", category="油画", price=5000, sort_order=3, status=1)
  INSERT course(name="已下架课程", category="国画", price=1000, sort_order=0, status=0)

  // Act
  response = GET /api/course/list

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data).hasSize(3)  // 仅返回上架课程
  assertThat(response.data[0].name).isEqualTo("素描基础班")  // sortOrder=1
  assertThat(response.data[1].name).isEqualTo("水彩提高班")  // sortOrder=2
  assertThat(response.data[2].name).isEqualTo("油画大师班")  // sortOrder=3
  // 已下架课程不在列表中
```

### 3.2 TP-CRS-008: 空课程列表

```
TEST: "GET /api/course/list - 全部下架时返回空数组"
  关联: AC-CRS-001（空课程列表）, B4

  // Arrange
  INSERT course(name="唯一课程", status=0)  // 下架

  // Act
  response = GET /api/course/list

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data).isEmpty()
```

### 3.3 TP-CRS-009: 分类筛选

```
TEST: "GET /api/course/list?category=素描 - 仅返回素描类课程"
  关联: EARS-CRS-002, AC-CRS-002（按类别筛选课程）

  // Arrange
  INSERT course(name="素描1", category="素描", sort_order=1, status=1)
  INSERT course(name="素描2", category="素描", sort_order=2, status=1)
  INSERT course(name="水彩1", category="水彩", sort_order=3, status=1)

  // Act
  response = GET /api/course/list?category=素描

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data).hasSize(2)
  assertThat(response.data).allMatch(c -> c.category == "素描")

---

TEST: "GET /api/course/list?category= - 空类别返回全部"
  关联: EARS-CRS-002, AC-CRS-002

  // Arrange (同上)

  // Act
  response = GET /api/course/list?category=

  // Assert
  assertThat(response.data).hasSize(3)  // 全部返回
```

### 3.4 TP-CRS-010: 课程详情（存在）

```
TEST: "GET /api/course/1 - 返回课程详情"
  关联: EARS-CRS-003, AC-CRS-003（查看课程详情）

  // Arrange
  INSERT course(id=1, name="素描基础班", category="素描", price=2000,
    duration="3个月", suitable_for="零基础学员",
    description="<p>课程描述</p>", cover_img="/images/sketch.jpg",
    sort_order=1, status=1)

  // Act
  response = GET /api/course/1

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data.id).isEqualTo(1)
  assertThat(response.data.name).isEqualTo("素描基础班")
  assertThat(response.data.price).isEqualTo(2000)
  assertThat(response.data.duration).isEqualTo("3个月")
  assertThat(response.data.suitableFor).isEqualTo("零基础学员")
```

### 3.5 TP-CRS-011: 课程详情（不存在）

```
TEST: "GET /api/course/999 - 课程不存在时 data 为 null"
  关联: EARS-CRS-003

  // Arrange (no course with id=999)

  // Act
  response = GET /api/course/999

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data).isNull()
```

### 3.6 TP-CRS-012: 管理员获取全部课程

```
TEST: "GET /api/admin/courses - 管理员看到全部课程（含下架）"
  关联: EARS-CRS-005

  // Arrange
  INSERT course(name="上架课程", status=1, sort_order=2)
  INSERT course(name="下架课程", status=0, sort_order=1)
  token = adminJwtToken()

  // Act
  response = GET /api/admin/courses, headers: {Authorization: "Bearer " + token}

  // Assert
  assertThat(response.code).isEqualTo(0)
  assertThat(response.data).hasSize(2)  // 包含已下架
  assertThat(response.data[0].name).isEqualTo("下架课程")  // sortOrder=1 排前面
```

### 3.7 TP-CRS-013: 新增课程

```
TEST: "POST /api/admin/course - 新增课程成功"
  关联: EARS-CRS-005, AC-CRS-004

  // Arrange
  token = adminJwtToken()
  body = {
    "name": "国画入门班",
    "category": "国画",
    "price": 2500,
    "duration": "2个月",
    "suitableFor": "零基础",
    "coverImg": "/images/guohua.jpg",
    "sortOrder": 5
  }

  // Act
  response = POST /api/admin/course, headers: {Authorization: "Bearer " + token}, body: body

  // Assert
  assertThat(response.code).isEqualTo(0)
  // 验证数据库中新增了一条记录
  courses = SELECT * FROM course WHERE name="国画入门班"
  assertThat(courses).hasSize(1)
  assertThat(courses[0].status).isEqualTo(1)  // 默认上架
```

### 3.8 TP-CRS-016: 上下架切换

```
TEST: "PUT /api/admin/course/1/status - 下架课程"
  关联: EARS-CRS-005, AC-CRS-004（管理员下架课程）

  // Arrange
  INSERT course(id=1, name="素描班", status=1)
  token = adminJwtToken()

  // Act
  response = PUT /api/admin/course/1/status, headers: {Authorization: "Bearer " + token}, body: {"status": 0}

  // Assert
  assertThat(response.code).isEqualTo(0)
  // 验证用户端不可见
  listResponse = GET /api/course/list
  assertThat(listResponse.data).isEmpty()  // 下架后列表为空
  // 验证管理端仍可见
  adminResponse = GET /api/admin/courses, headers: {Authorization: "Bearer " + token}
  assertThat(adminResponse.data).hasSize(1)
  assertThat(adminResponse.data[0].status).isEqualTo(0)
```

### 3.9 TP-CRS-017: 认证拦截

```
TEST: "GET /api/admin/courses - 无 Token 返回 401"
  关联: EARS-CRS-005

  // Arrange (no token)

  // Act
  response = GET /api/admin/courses

  // Assert
  assertThat(response.httpStatus).isEqualTo(401)

---

TEST: "GET /api/admin/courses - 非管理员返回 403"
  关联: EARS-CRS-005

  // Arrange
  token = normalUserJwtToken()  // 普通用户 token

  // Act
  response = GET /api/admin/courses, headers: {Authorization: "Bearer " + token}

  // Assert
  assertThat(response.httpStatus).isEqualTo(403)
```

### 3.10 TP-CRS-019: 免费课程（边界）

```
TEST: "POST /api/admin/course - price=0 免费课程 - 保存成功"
  关联: B1

  // Arrange
  token = adminJwtToken()
  body = { "name": "免费体验课", "category": "素描", "price": 0, "duration": "1课时", "suitableFor": "所有人" }

  // Act
  response = POST /api/admin/course, headers: {Authorization: "Bearer " + token}, body: body

  // Assert
  assertThat(response.code).isEqualTo(0)
  courses = SELECT * FROM course WHERE name="免费体验课"
  assertThat(courses[0].price).isEqualTo(0)
```

---

## 4. 测试数据设计

### 4.1 基础测试数据

```
TestData: COURSE_SKETCH
  name: "素描基础班"
  category: "素描"
  description: "<p>零基础入门</p>"
  price: 2000
  duration: "3个月"
  suitableFor: "零基础学员"
  coverImg: "/images/sketch.jpg"
  sortOrder: 1
  status: 1

TestData: COURSE_WATERCOLOR
  name: "水彩提高班"
  category: "水彩"
  description: "<p>水彩进阶</p>"
  price: 3000
  duration: "2个月"
  suitableFor: "有基础学员"
  coverImg: "/images/watercolor.jpg"
  sortOrder: 2
  status: 1

TestData: COURSE_OIL
  name: "油画大师班"
  category: "油画"
  price: 5000
  duration: "4个月"
  suitableFor: "有素描基础"
  sortOrder: 3
  status: 1

TestData: COURSE_INACTIVE
  name: "已下架课程"
  category: "国画"
  price: 1000
  duration: "1个月"
  suitableFor: "所有人"
  sortOrder: 0
  status: 0
```

### 4.2 管理员测试用户

```
TestData: ADMIN_USER
  openid: "test_admin_openid"
  // 需在 admin_whitelist 表中注册

TestData: NORMAL_USER
  openid: "test_normal_openid"
  // 不在 admin_whitelist 表中
```

---

## 5. 优先级矩阵

| 优先级 | 测试点 | 数量 | 说明 |
|--------|--------|------|------|
| P0 | TP-CRS-001, 002, 003, 005, 007, 009, 010, 012, 013, 014, 015, 016, 017 | 13 | 核心 CRUD 功能 + 认证 |
| P1 | TP-CRS-004, 006, 008, 011, 018 | 5 | 边界条件 + 异常路径 |
| P2 | TP-CRS-019, 020 | 2 | 极端边界 |

---

## 6. 需求追溯

| 需求编号 | 测试点编号 | 覆盖说明 |
|---------|-----------|---------|
| EARS-CRS-001 | TP-CRS-007, 008, 018 | 课程列表展示、空列表、排序 |
| EARS-CRS-002 | TP-CRS-009, 020 | 分类筛选、不存在的类别 |
| EARS-CRS-003 | TP-CRS-010, 011 | 课程详情（存在/不存在） |
| EARS-CRS-004 | TP-CRS-007 | 首页课程入口（复用列表接口） |
| EARS-CRS-005 | TP-CRS-001~006, 012~017 | 管理后台全部 CRUD 操作 |
| AC-CRS-001 | TP-CRS-007, 008 | 查看课程列表 + 空课程列表 |
| AC-CRS-002 | TP-CRS-009 | 按类别筛选 |
| AC-CRS-003 | TP-CRS-010 | 查看课程详情 |
| AC-CRS-004 | TP-CRS-013, 016 | 管理员添加 + 下架课程 |
| B1 | TP-CRS-019 | 免费课程 price=0 |
| B4 | TP-CRS-008 | 所有课程下架 |
| B5 | TP-CRS-020 | 类别不存在 |
| B6 | TP-CRS-004, 015 | 删除幂等性 |
