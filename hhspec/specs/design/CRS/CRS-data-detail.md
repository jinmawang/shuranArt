# CRS 数据层详细设计

> **L2 详设** -- 基于 L1 领域模型 `domain-model.md` 和现有 Teacher 数据层模式细化。

## 1. Course Entity 设计

### 1.1 实体定义

参照 `Teacher.java` 实体模式，Course 实体映射到 `course` 数据库表。

```
Entity: Course
Table: "course"
Annotations: @Data, @TableName("course")

Fields:
  id          : Long           @TableId(type = IdType.AUTO)   -- 自增主键
  name        : String                                         -- 课程名称
  category    : String                                         -- 课程类别（素描/水彩/油画/国画等）
  description : String                                         -- 课程详细描述（富文本 HTML），可空
  price       : Integer                                        -- 课程价格（单位：元），0=免费体验课
  duration    : String                                         -- 课程时长描述（如"3个月"、"12课时"）
  suitableFor : String                                         -- 适合人群描述
  coverImg    : String                                         -- 课程封面图 URL，可空
  sortOrder   : Integer                                        -- 排序序号，升序排列
  status      : Integer                                        -- 状态：1=上架, 0=下架
  createdAt   : LocalDateTime                                  -- 创建时间
```

**设计说明**：
- 字段命名遵循现有 camelCase 规范，MyBatis Plus 自动映射到 snake_case 数据库列。
- 与 Teacher 实体结构对齐：都有 `id`, `sortOrder`, `status`, `createdAt`。
- `description` 使用 String 映射 TEXT 类型（与 Teacher.intro 一致）。
- 不需要 `@TableField(typeHandler = ...)` 注解（无 JSON 字段，比 Teacher.works 简单）。
- 不需要 `@TableField(exist = false)` 计算字段（比 Activity 简单）。

### 1.2 字段与数据库列映射

| Java 字段 | DB 列名 | DB 类型 | 约束 | 默认值 | 说明 |
|-----------|---------|---------|------|--------|------|
| id | id | BIGINT | PK, AUTO_INCREMENT | -- | 自增主键 |
| name | name | VARCHAR(64) | NOT NULL | -- | 课程名称 |
| category | category | VARCHAR(32) | NOT NULL | -- | 课程类别 |
| description | description | TEXT | nullable | NULL | 富文本描述 |
| price | price | INT | NOT NULL | -- | 价格（元） |
| duration | duration | VARCHAR(32) | NOT NULL | -- | 时长描述 |
| suitableFor | suitable_for | VARCHAR(64) | NOT NULL | -- | 适合人群 |
| coverImg | cover_img | VARCHAR(512) | nullable | NULL | 封面图 URL |
| sortOrder | sort_order | INT | -- | 0 | 排序序号 |
| status | status | TINYINT | -- | 1 | 上架状态 |
| createdAt | created_at | DATETIME | -- | CURRENT_TIMESTAMP | 创建时间 |

---

## 2. CourseMapper 设计

### 2.1 Mapper 接口

参照 `TeacherMapper.java` 模式，CourseMapper 继承 MyBatis Plus 的 BaseMapper。

```
Interface: CourseMapper extends BaseMapper<Course>
Annotations: @Mapper
Custom Methods: 无（全部使用 BaseMapper 内置方法）
```

**设计说明**：
- 与 TeacherMapper 完全一致的模式：纯接口声明，无自定义 SQL。
- 所有查询通过 `LambdaQueryWrapper` 在 Controller/Service 层构造。
- BaseMapper 提供的方法满足全部 CRUD 需求：
  - `selectList(queryWrapper)` -- 列表查询（EP-01, EP-03）
  - `selectById(id)` -- 详情查询（EP-02）
  - `insert(entity)` -- 新增（EP-04）
  - `updateById(entity)` -- 编辑/状态更新（EP-04, EP-06）
  - `deleteById(id)` -- 删除（EP-05）

### 2.2 查询场景汇总

| 编号 | 方法 | 条件 | 调用方 | 对应端点 |
|------|------|------|--------|---------|
| RM-001 | selectList | status=1, ORDER BY sort_order ASC, [AND category=?] | CourseController | EP-01 |
| RM-002 | selectById | id=? | CourseController | EP-02 |
| RM-003 | selectList | ORDER BY sort_order ASC（全部状态） | AdminService | EP-03 |
| RM-004 | insert | -- | AdminService | EP-04 (新增) |
| RM-005 | updateById | id=? | AdminService | EP-04 (编辑), EP-06 |
| RM-006 | deleteById | id=? | AdminService | EP-05 |

---

## 3. DTO/Entity 转换规则

### 3.1 无独立 DTO -- 直接使用 Entity

**设计决策**：CRS 领域不引入独立 DTO 类，直接使用 Course Entity 作为请求体和响应体。

**依据**：
- 参照现有 Teacher 模式：`AdminController.saveTeacher(@RequestBody Teacher teacher)` 直接接收 Entity。
- 参照现有 Activity 模式：`Result<Activity>` 直接返回 Entity。
- CRS 场景简单（纯 CRUD），输入字段与 Entity 字段完全对齐，无需转换层。

### 3.2 JSON 序列化映射

MyBatis Plus + Jackson 自动处理 camelCase (Java) <-> snake_case (DB) 映射：

| 请求/响应 JSON 字段 | Java Entity 字段 | DB 列名 |
|--------------------|-----------------|---------|
| id | id | id |
| name | name | name |
| category | category | category |
| description | description | description |
| price | price | price |
| duration | duration | duration |
| suitableFor | suitableFor | suitable_for |
| coverImg | coverImg | cover_img |
| sortOrder | sortOrder | sort_order |
| status | status | status |
| createdAt | createdAt | created_at |

**注意**：Spring Boot 默认 Jackson 配置将 Java camelCase 序列化为 JSON camelCase。MyBatis Plus 的 `mapUnderscoreToCamelCase=true`（默认启用）处理 DB snake_case 到 Java camelCase 的映射。

---

## 4. 数据库 Migration SQL

### 4.1 新建 course 表

```sql
-- CRS: 课程表
-- 参照 teacher 表结构设计，字段风格保持一致
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(64) NOT NULL COMMENT '课程名称',
    `category` VARCHAR(32) NOT NULL COMMENT '课程类别（素描/水彩/油画/国画等）',
    `description` TEXT COMMENT '课程详细描述（富文本HTML）',
    `price` INT NOT NULL DEFAULT 0 COMMENT '课程价格（单位：元），0=免费体验课',
    `duration` VARCHAR(32) NOT NULL COMMENT '课程时长描述',
    `suitable_for` VARCHAR(64) NOT NULL COMMENT '适合人群描述',
    `cover_img` VARCHAR(512) COMMENT '课程封面图URL',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号，升序排列',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1=上架，0=下架',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';
```

### 4.2 初始数据（可选，用于演示）

```sql
-- CRS: 插入示例课程数据
INSERT INTO `course` (`name`, `category`, `description`, `price`, `duration`, `suitable_for`, `cover_img`, `sort_order`, `status`) VALUES
('素描基础班', '素描', '<p>零基础入门素描课程，从握笔姿势开始，系统学习素描基本功。</p>', 2000, '3个月', '零基础学员', '/images/course-sketch.jpg', 1, 1),
('水彩提高班', '水彩', '<p>水彩进阶技法课程，学习水彩的色彩搭配与湿画法。</p>', 3000, '2个月', '有绘画基础学员', '/images/course-watercolor.jpg', 2, 1),
('油画大师班', '油画', '<p>油画创作课程，从写生到创作的完整训练体系。</p>', 5000, '4个月', '有素描基础学员', '/images/course-oil.jpg', 3, 1);
```

### 4.3 与 init.sql 的集成方式

**方案**：将建表语句添加到 `mysql/init.sql` 文件末尾（在现有 `studio_config` INSERT 之后），或作为独立 migration 文件 `mysql/migration_v5_course.sql`。

**推荐**：独立 migration 文件，避免修改已有 init.sql 影响存量部署。

---

## 5. 查询优化

### 5.1 索引设计

```sql
-- 课程列表查询优化（EP-01: WHERE status=1 AND category=? ORDER BY sort_order）
-- 数据量预估：< 50 条课程，无需额外索引
-- sort_order 排序在小数据量下全表扫描即可满足性能要求
```

**设计决策**：当前不添加额外索引。

**依据**：
- 课程数据量极小（预估 < 50 条），全表扫描 + filesort 性能足够。
- 参照 Teacher 表：未添加任何业务索引（仅有主键索引），运行良好。
- 如果后续课程数量增长到 100+ 且有性能问题，可添加复合索引 `(status, sort_order)`。

### 5.2 无分页设计

**设计决策**：课程列表不使用分页，一次返回全部上架课程。

**依据**：
- 参照 Teacher 列表（一次全量返回）和 Activity 列表（一次全量返回）。
- 画室课程数量有限（通常 5-20 个），无需分页。

---

## 6. 事务边界

### 6.1 事务场景分析

| 端点 | 是否需要事务 | 说明 |
|------|------------|------|
| EP-01 GET /api/course/list | 否 | 只读查询 |
| EP-02 GET /api/course/{id} | 否 | 只读查询 |
| EP-03 GET /api/admin/courses | 否 | 只读查询 |
| EP-04 POST /api/admin/course | 否 | 单表单次写操作 |
| EP-05 DELETE /api/admin/course/{id} | 否 | 单表单次写操作 |
| EP-06 PUT /api/admin/course/{id}/status | 否 | 单表单次写操作（selectById + updateById 为非原子操作，但并发更新场景几乎不存在） |

**设计决策**：CRS 领域全部端点不需要 `@Transactional` 注解。

**依据**：
- 参照 AdminService 中 Teacher 相关方法：`saveTeacher`、`deleteTeacher` 均未标注 `@Transactional`。
- CRS 场景为单管理员操作，无并发写入风险。
- 每个写操作只涉及单表单次操作，无需事务保证。

---

## 7. 数据校验规则汇总

| 编号 | 字段 | 规则 | 校验位置 | 失败行为 |
|------|------|------|---------|---------|
| CV-001 | name | 非空，maxLength=64 | AdminService | throw RuntimeException |
| CV-002 | category | 非空，maxLength=32 | AdminService | throw RuntimeException |
| CV-003 | price | 非 null，>= 0 | AdminService | throw RuntimeException |
| CV-004 | duration | 非空，maxLength=32 | AdminService | throw RuntimeException |
| CV-005 | suitableFor | 非空，maxLength=64 | AdminService | throw RuntimeException |
| CV-006 | status | 0 或 1（EP-04, EP-06） | AdminService | throw RuntimeException |
| CV-007 | course.id | 存在性校验（EP-06） | AdminService | throw RuntimeException |

**注意**：maxLength 校验在数据库层面由 VARCHAR 约束兜底。Service 层可选择不做 maxLength 校验（与现有 Teacher 保存行为一致，Teacher 也未做长度校验）。MVP 阶段优先保证非空校验。

---

## 8. 需求追溯

| 需求编号 | 数据层设计覆盖 | 说明 |
|---------|--------------|------|
| EARS-CRS-001 | RM-001, course 表 | 课程列表数据来源 |
| EARS-CRS-002 | RM-001 (category 条件) | 分类筛选数据查询 |
| EARS-CRS-003 | RM-002 | 课程详情数据查询 |
| EARS-CRS-004 | RM-001 | 首页课程入口复用列表查询 |
| EARS-CRS-005 | RM-003~RM-006, CV-001~CV-007 | 管理端 CRUD 全部数据操作 |
| B1 | CV-003 (price >= 0) | 允许 price=0 |
| B2 | description nullable | 描述字段允许为空 |
| B3 | coverImg nullable | 封面图允许为空 |
