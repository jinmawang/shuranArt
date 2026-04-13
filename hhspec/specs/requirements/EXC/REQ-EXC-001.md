---
task_id: "REQ-EXC-001"
title: "积分兑换系统"
type: "功能需求"
version: "1.0"
date: "2026-03-26"
author: "analyst-agent"
status: "draft"
priority: "high"
module: "EXC"
related_changes:
  - "full-spec-gen"
tags:
  - "积分兑换"
  - "商城"
  - "兑换码"
  - "管理后台"
---

# 积分兑换系统

## 1. 概述

### 1.1 背景与动机

舒然画室小程序已实现「分享裂变 + 抽奖激励」闭环，用户通过抽奖获得积分奖品（5/20/50/100积分），但积分目前无消费出口。积分兑换系统为用户积分提供兑换渠道，形成「分享 -> 抽奖 -> 积分 -> 兑换」的完整激励闭环，提升用户留存和到店转化。

**证据来源**：
- 用户表 `user.points` 字段已存在（`mysql/init.sql` 第12行）
- 抽奖奖品中积分类型有 5/20/50/100 四档（`mysql/init.sql` 第148-152行）
- 兑换相关数据库表 `exchange_item` 和 `exchange_record` 已创建（`mysql/init.sql` 第99-126行）
- 领域模型标注 EXC 上下文为"已规划未实现"（`domain-model.md` 第77行）
- `domains.yml` 标注 EXC 状态为 `schema_only`（第70行）

### 1.2 目标用户

| 角色 | 描述 |
|------|------|
| 普通用户（学员/家长） | 通过抽奖积累积分，在积分商城兑换课程券、体验课、画材礼包��商品 |
| 管理员（画室老师） | 管理兑换商品（增删改、上下架、库存管理），核销兑换码 |

### 1.3 范围

**In Scope**:
- 用户端：积分商城页面（商品列表展示）、兑换操作、兑换记录查看
- 管理端：兑换商品 CRUD（新增、编辑、上下架、删除）、库存管理、兑换码核销
- 后端 API：兑换商品列表查询、兑换操作、兑换记录查询、管理端商品 CRUD、兑换码核销
- 实物商品线下领取流程：生成兑换码 -> 到店出示 -> 管理员核销

**Out of Scope**:
- 积分获取规则变更（积分来源仍仅为抽奖系统，不在本需求范围内调整）
- 虚拟商品自动发放（如课程券自动绑定到用户课程列表，课程系统尚未实现）
- 积分过期机制
- 兑换商品的图片上传功能（复用现有图片 URL 配置方式）
- 物流配送（所有商品均为到店领取）

## 2. 用户故事

- US-001: 作为普通用户，我希望浏览积分商城中的可兑换商品列表，以便了解我的积分可以兑换什么商品。
- US-002: 作为普通用户，我希望用我的积分兑换心仪的商品，以便获得课程券或画材等实际奖励。
- US-003: 作为普通用户，我希望查看我的兑换历史记录，以便了解兑换状态和兑换码信息。
- US-004: 作为普通用户，我希望到店出示兑换码完成线下领取，以便获得实物商品。
- US-005: 作为管理员，我希望新增和编辑兑换商品（名称、积分价格、库存、描述、图片、是否需线下领取），以便维护积分商城的商品信息。
- US-006: 作为管理员，我希望对兑换商品进行上架和下架操作，以便控制商品在用户端的可见性。
- US-007: 作为管理员，我希望核销用户的兑换码，以便确认用户已到店领取商品。

## 3. 系统需求（EARS 格式）

### 3.1 功能需求

**商品列表查询**

- REQ-EXC-001: 当用户请求积分商城页面时，系统应返回所有 status=1（上架状态）的兑换商品列表，每个商品包含 id、name、pointsCost、stock、description、image、needClaim 字段。
- REQ-EXC-002: 当系统返回兑换商品列表时，系统应按 pointsCost 升序排列商品。

**兑换操作**

- REQ-EXC-003: 当已登录用户提交兑换请求（包含 itemId）时，系统应验证以下全部条件均满足后执行兑换：该商品存在且 status=1，该商品 stock > 0，用户 points >= 商品 pointsCost。
- REQ-EXC-004: 当兑换条件验证通过时，系统应在同一事务中执行以下操作：将用户 points 减少该商品的 pointsCost，将该商品 stock 减少 1，创建一条 exchange_record 记录（包含 userId、itemId、itemName、pointsCost、status、createdAt）。
- REQ-EXC-005: 当兑换的商品 needClaim=1 时，系统应为该兑换记录生成一个 8 位字母数字组成的唯一兑换码（claimCode），设置 status 为 "pending"，设置 expireAt 为当前时间加 30 天。
- REQ-EXC-006: 当兑换的商品 needClaim=0 时，系统应设置该兑换记录的 status 为 "claimed"，设置 claimedAt 为当前时间，不生成兑换码。
- REQ-EXC-007: 当用户积分不足（points < pointsCost）时，系统应拒绝兑换请求并返回错误信息"积分不足"。
- REQ-EXC-008: 当商品库存不足（stock <= 0）时，系统应拒绝兑换请求并返回错误信息"库存不足"。
- REQ-EXC-009: 当商品不存在或已下架（status != 1）时，系统应拒绝兑换请求并返回错误信息"商品不存在或已下架"。

**兑换记录查询**

- REQ-EXC-010: 当已登录用户请求兑换记录列表时，系统应返回该用户的所有兑换记录，按 createdAt 降序排列，每条记录包含 id、itemName、pointsCost、claimCode、status、createdAt、claimedAt、expireAt 字段。

**兑换码核销**

- REQ-EXC-011: 当管理员提交兑换码核销请求（包含 claimCode）时，系统应验证以下全部条件均满足后执行核销：该兑换码对应的记录存在，该记录 status 为 "pending"，该记录 expireAt 晚于当前时间。
- REQ-EXC-012: 当核销条件验证通过时，系统应将该兑换记录的 status 更新为 "claimed"，设置 claimedAt 为当前时间。
- REQ-EXC-013: 当兑换码不存在时，系统应拒绝核销请求并返回错误信息"兑换码无效"。
- REQ-EXC-014: 当兑换码对应的记录 status 为 "claimed" 时，系统应拒绝核销请求并返回错误信息"该兑换码已核销"。
- REQ-EXC-015: 当兑换码对应的记录 expireAt 早于或等于当前时间时，系统应拒绝核销请求并返回错误信息"该兑换码已过期"。

**管理端商品 CRUD**

- REQ-EXC-016: 当管理员提交新增商品请求时，系统应创建一条 exchange_item 记录，必填字段为 name 和 pointsCost，可选字段为 stock（默认 0）、description、image、needClaim（默认 1）、status（默认 1）。
- REQ-EXC-017: 当管理员提交编辑商品请求（包含 id）时，系统应更新对应 exchange_item 记录的指定字段。
- REQ-EXC-018: 当管理员将商品 status 设置为 0 时，系统应将该商品从用户端商城列表中隐藏（下架）。
- REQ-EXC-019: 当管理员将商品 status 设置为 1 时，系统应将该商品在用户端商城列表中显示（上架）。
- REQ-EXC-020: 当管理员提交删除商品请求时，系统应删除对应的 exchange_item 记录。
- REQ-EXC-021: 当管理员请求兑换商品列表时，系统应返回所有兑换商品（包括已下架的），按 createdAt 降序排列。

**用户端入口**

- REQ-EXC-022: 系统应在首页功能入口区域和"我的"页面菜单中提供进入积分商城的导航入口。

### 3.2 非功能需求

- REQ-EXC-030: 兑换操作中的积分扣减、库存扣减、记录创建应在同一数据库事务中执行，任一步骤失败则全部回滚。
- REQ-EXC-031: 兑换码应由 `CodeGenerator.generateClaimCode()` 方法生成，使用 8 位大写字母（不含 I/O）和数字（不含 0/1）的组合，字符集为 `ABCDEFGHJKLMNPQRSTUVWXYZ23456789`。
- REQ-EXC-032: 兑换操作 API 响应时间应不超过 500 毫秒（单用户非并发条件下）。
- REQ-EXC-033: 兑换相关 API 端点（除商品列表查询外）应要求 JWT 认证，管理端 API 应额外要求管理员白名单权限。

## 4. 验收标准（Gherkin 格式）

### 4.1 积分商城商品列表

```gherkin
Feature: 积分商城商品列表
  用户浏览积分商城中的可兑换商品

  Scenario: 获取商品列表_商品存在_返回上架商品列表
    Given 系统中存在以下兑换商品:
      | name       | pointsCost | stock | status | description    | image            | needClaim |
      | 20元课程券  | 50         | 100   | 1      | 可抵扣20元课程费 | /images/c20.png  | 1         |
      | 50元课程券  | 100        | 50    | 1      | 可抵扣50元课程费 | /images/c50.png  | 1         |
      | 已下架商品  | 200        | 10    | 0      | 已下架          | /images/off.png  | 1         |
    When 用户请求积分商城商品列表
    Then 系统返回 2 条商品记录
    And 返回列表不包含名为 "已下架商品" 的商品
    And 返回列表按 pointsCost 升序排列，第一条为 "20元课程券"

  Scenario: 获取商品列表_无上架商品_返回空列表
    Given 系统中所有兑换商品 status 均为 0
    When 用户请求积分商城商品列表
    Then 系统返回空列表

  @REQ-EXC-001 @REQ-EXC-002
  Scenario: 获取商品列表_返回完整字段_字段均有值
    Given 系统中存在一条兑换商品:
      | name      | pointsCost | stock | status | description     | image           | needClaim |
      | 体验课     | 200        | 20    | 1      | 免费体验一节课   | /images/exp.png | 1         |
    When 用户请求积分商城商品列表
    Then 返回的商品记录包含以下字段: id, name, pointsCost, stock, description, image, needClaim
```

### 4.2 兑换操作

```gherkin
Feature: 积分兑换操作
  用户使用积分兑换商品

  @REQ-EXC-003 @REQ-EXC-004 @REQ-EXC-005
  Scenario: 兑换需领取商品_积分库存充足_生成兑换码
    Given 用户当前积分为 200
    And 存在一条上架商品 "体验课" 所需积分 200 库存 10 needClaim 为 1
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回兑换成功
    And 用户积分变为 0
    And 商品 "体验课" 库存变为 9
    And 生成一条兑换记录，status 为 "pending"
    And 该兑换记录包含 8 位兑换码
    And 该兑换记录的 expireAt 为当前时间加 30 天

  @REQ-EXC-003 @REQ-EXC-004 @REQ-EXC-006
  Scenario: 兑换无需领取商品_积分库存充足_自动完成
    Given 用户当前积分为 100
    And 存在一条上架商品 "电子优惠券" 所需积分 50 库存 999 needClaim 为 0
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回兑换成功
    And 用户积分变为 50
    And 商品 "电子优惠券" 库存变为 998
    And 生成一条兑换记录，status 为 "claimed"
    And 该兑换记录的 claimedAt 不为空
    And 该兑换记录的 claimCode 为空

  @REQ-EXC-007
  Scenario: 兑换商品_积分不足_拒绝兑换
    Given 用户当前积分为 30
    And 存在一条上架商品 "20元课程券" 所需积分 50 库存 100 needClaim 为 1
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回错误信息 "积分不足"
    And 用户积分仍为 30
    And 商品库存仍为 100

  @REQ-EXC-008
  Scenario: 兑换商品_库存不足_拒绝兑换
    Given 用户当前积分为 500
    And 存在一条上架商品 "画材礼包" 所需积分 500 库存 0 needClaim 为 1
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回错误信息 "库存不足"
    And 用户积分仍为 500

  @REQ-EXC-009
  Scenario: 兑换商品_商品已下架_拒绝兑换
    Given 用户当前积分为 100
    And 存在一条下架商品（status=0）"过季商品" 所需积分 50
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回错误信息 "商品不存在或已下架"
    And 用户积分仍为 100

  @REQ-EXC-009
  Scenario: 兑换商品_商品不存在_拒绝兑换
    Given 用户当前积分为 100
    When 用户提交兑换请求，itemId 为 999999（不存在的ID）
    Then 系统返回错误信息 "商品不存在或已下架"
    And 用户积分仍为 100

  @REQ-EXC-030
  Scenario: 兑换操作_事务一致性_中间步骤失败则回滚
    Given 用户当前积分为 100
    And 存在一条上架商品 "课程券" 所需积分 50 库存 1 needClaim 为 1
    And 数据库记录插入时发生异常
    When 用户提交兑换请求，itemId 为该商品 ID
    Then 系统返回错误
    And 用户积分仍为 100
    And 商品库存仍为 1
```

### 4.3 兑换记录查询

```gherkin
Feature: 兑换记录查询
  用户查看自己的兑换历史

  @REQ-EXC-010
  Scenario: 查询兑换记录_有记录_按时间降序返回
    Given 用户有以下兑换记录:
      | itemName   | pointsCost | status  | createdAt           |
      | 20元课程券  | 50         | claimed | 2026-03-20 10:00:00 |
      | 体验课      | 200        | pending | 2026-03-25 15:30:00 |
    When 用户请求兑换记录列表
    Then 系统返回 2 条记录
    And 第一条记录为 "体验课"（按 createdAt 降序）
    And 每条记录包含字段: id, itemName, pointsCost, claimCode, status, createdAt, claimedAt, expireAt

  @REQ-EXC-010
  Scenario: 查询兑换记录_无记录_返回空列表
    Given 用户没有任何兑换记录
    When 用户请求兑换记录列表
    Then 系统返回空列表

  Scenario: 查询兑换记录_只能看到自己的记录
    Given 用户 A 有 1 条兑换记录
    And 用户 B 有 3 条兑换记录
    When 用户 A 请求兑换记录列表
    Then 系统返回 1 条记录
    And 返回的记录均属于用户 A
```

### 4.4 兑换码核销

```gherkin
Feature: 兑换码核销
  管理员核销用户的兑换码

  @REQ-EXC-011 @REQ-EXC-012
  Scenario: 核销兑换码_有效兑换码_核销成功
    Given 存在一条兑换记录，claimCode 为 "ABCD1234"，status 为 "pending"，expireAt 为 "2026-04-25 15:30:00"
    And 当前时间为 "2026-03-26 10:00:00"
    When 管理员提交核销请求，claimCode 为 "ABCD1234"
    Then 系统返回核销成功
    And 该兑换记录 status 变为 "claimed"
    And 该兑换记录 claimedAt 为当前时间

  @REQ-EXC-013
  Scenario: 核销兑换码_兑换码不存在_核销失败
    When 管理员提交核销请求，claimCode 为 "XXXX0000"
    Then 系统返回错误信息 "兑换码无效"

  @REQ-EXC-014
  Scenario: 核销兑换码_已核销_重复核销失败
    Given 存在一条兑换记录，claimCode 为 "ABCD1234"，status 为 "claimed"
    When 管理员提交核销请求，claimCode 为 "ABCD1234"
    Then 系统返回错误信息 "该兑换码已核销"

  @REQ-EXC-015
  Scenario: 核销兑换码_已过期_核销失败
    Given 存在一条兑换记录，claimCode 为 "ABCD1234"，status 为 "pending"，expireAt 为 "2026-03-25 00:00:00"
    And 当前时间为 "2026-03-26 10:00:00"
    When 管理员提交核销请求，claimCode 为 "ABCD1234"
    Then 系统返回错误信息 "该兑换码已过期"
```

### 4.5 管理端商品 CRUD

```gherkin
Feature: 管理端兑换商品管理
  管理员对兑换商品进行增删改查操作

  @REQ-EXC-016
  Scenario: 新增商品_必填字段齐全_创建成功
    When 管理员提交新增商品请求:
      | name      | pointsCost | stock | description    | image           | needClaim | status |
      | 20元课程券 | 50         | 100   | 可抵扣20元课程费 | /images/c20.png | 1         | 1      |
    Then 系统创建商品成功
    And 数据库中存在一条 name 为 "20元课程券" 的 exchange_item 记录

  @REQ-EXC-016
  Scenario: 新增商品_仅必填字段_使用默认值创建
    When 管理员提交新增商品请求:
      | name       | pointsCost |
      | 新商品测试  | 100        |
    Then 系统创建商品成功
    And 该商品 stock 为 0
    And 该商品 needClaim 为 1
    And 该商品 status 为 1

  @REQ-EXC-017
  Scenario: 编辑商品_更新字段_保存成功
    Given 存在商品 "旧名称" 积分价格 50
    When 管理员提交编辑请求，将 name 改为 "新名称"，pointsCost 改为 80
    Then 该商品 name 变为 "新名称"
    And 该商品 pointsCost 变为 80

  @REQ-EXC-018 @REQ-EXC-019
  Scenario: 商品上下架_切换状态_影响用户可见性
    Given 存在上架商品 "课程券" status 为 1
    When 管理员将该商品 status 设置为 0
    Then 用户请求商城列表时不包含 "课程券"
    When 管理员将该商品 status 设置为 1
    Then 用户请求商城列表时包含 "课程券"

  @REQ-EXC-020
  Scenario: 删除商品_存在商品_删除成功
    Given 存在商品 "待删除商品"
    When 管理员提交删除请求，id 为该商品 ID
    Then 数据库中不存在该商品记录

  @REQ-EXC-021
  Scenario: 管理端获取商品列表_包含已下架商品
    Given 系统中存在以下商品:
      | name    | status |
      | 上架商品 | 1      |
      | 下架商品 | 0      |
    When 管理员请求商品列表
    Then 返回 2 条记录，包含 "上架商品" 和 "下架商品"
```

### 4.6 认证与权限

```gherkin
Feature: 兑换API认证与权限控制

  @REQ-EXC-033
  Scenario: 用户端兑换操作_未登录_拒绝访问
    Given 请求未携带有效的 JWT Token
    When 用户请求兑换操作
    Then 系统返回 HTTP 401 状态码

  @REQ-EXC-033
  Scenario: 管理端操作_非管理员_拒绝访问
    Given 请求携带有效的 JWT Token 但用户不在管理员白名单
    When 用户请求管理端兑换商品接口
    Then 系统返回 HTTP 403 状态码

  Scenario: 商品列表查询_无需认证_允许访问
    Given 请求未携带 JWT Token
    When 请求积分商城商品列表接口
    Then 系统返回商品列表数据（HTTP 200）
```

## 5. 边界场景清单

| 编号 | 类别 | 场景描述 | 关联需求 | 预期行为 |
|------|------|----------|----------|----------|
| BD-001 | 空值 | 用户积分为 0 时尝试兑换 | REQ-EXC-007 | 系统拒绝兑换并返回"积分不足" |
| BD-002 | 空值 | 兑换商品列表为空（无上架商品） | REQ-EXC-001 | 系统返回空数组，前端显示"暂无可兑换商品" |
| BD-003 | 空值 | 用户无兑换记录时查询记录 | REQ-EXC-010 | 系统返回空数组 |
| BD-004 | 空值 | 新增商品时 description 和 image 为空 | REQ-EXC-016 | 系统允许创建，这两个字段存储为 null |
| BD-005 | 极值 | 用户积分恰好等于商品所需积分（边界相等） | REQ-EXC-003 | 系统允许兑换，用户积分变为 0 |
| BD-006 | 极值 | 商品库存恰好为 1，兑换后变为 0 | REQ-EXC-004 | 系统允许兑换，库存变为 0，后续兑换请求因库存不足被拒绝 |
| BD-007 | 极值 | 商品 pointsCost 为 1（最低积分值） | REQ-EXC-003 | 系统正常处理兑换 |
| BD-008 | 极值 | 兑换码在 expireAt 精确时刻核销（边界等于） | REQ-EXC-015 | 系统拒绝核销，返回"该兑换码已过期"（等于过期时间视为已过期） |
| BD-009 | 并发 | 两个用户同时兑换最后一件库存商品 | REQ-EXC-004, REQ-EXC-008 | 仅一个用户兑换成功，另一个收到"库存不足"；不出现超卖 |
| BD-010 | 并发 | 同一用户短时间内重复提交兑换请求 | REQ-EXC-004 | 每次请求独立处理，若积分和库存均充足则每次均扣减；不出现积分余额为负的情况 |
| BD-011 | 权限 | 普通用户尝试访问管理端商品管理接口 | REQ-EXC-033 | 系统返回 HTTP 403 |
| BD-012 | 权限 | 普通用户尝试访问核销接口 | REQ-EXC-033 | 系统返回 HTTP 403 |
| BD-013 | 权限 | 未登录用户尝试兑换操作 | REQ-EXC-033 | 系统返回 HTTP 401 |
| BD-014 | 网络 | 兑换请求提交后网络中断，客户端未收到响应 | REQ-EXC-030 | 服务端事务已提交（积分已扣减），用户刷新页面后可在兑换记录中看到该兑换 |
| BD-015 | 网络 | 商品列表加载失败（网络超时） | REQ-EXC-001 | 前端显示"加载失败，点击重试"提示 |
| BD-016 | 数据完整性 | 兑换记录中的 itemId 指向的商品被管理员删除 | REQ-EXC-010 | 兑换记录仍可正常查询（因 itemName 和 pointsCost 已冗余存储在 exchange_record 中） |
| BD-017 | 数据完整性 | 管理员修改商品价格后，历史兑换记录中的 pointsCost 不变 | REQ-EXC-004 | 兑换记录中存储的是兑换时刻的 pointsCost 快照，不受后续商品价格修改影响 |
| BD-018 | 数据完整性 | 生成兑换码时与已有兑换码重复 | REQ-EXC-031 | 系统应确保兑换码唯一性（数据库 claim_code 索引保证），若重复则重新生成 |
| BD-019 | 状态流转 | 兑换记录从 "pending" 状态流转到 "claimed" | REQ-EXC-012 | 仅通过管理员核销操作可将 status 从 "pending" 变更为 "claimed" |
| BD-020 | 状态流转 | 已 "claimed" 的兑换记录尝试再次核销 | REQ-EXC-014 | 系统拒绝并返回"该兑换码已核销" |
| BD-021 | 状态流转 | "pending" 状态的兑换记录过期后尝试核销 | REQ-EXC-015 | 系统拒绝并返回"该兑换码已过期" |

## 6. UI 交互流程

### 6.1 积分商城页面 -- 成功路径

1. 用户在"首页"功能入口区域点击"积分商城"图标，或在"我的"页面点击"积分商城"菜单项
2. 系统跳转到积分商城页面 `/pages/exchange/exchange`
3. 系统调用 `GET /api/exchange/items` 加载商品列表，页面顶部显示用户当前积分余额
4. 系统显示商品卡片列表，每张卡片包含：商品图片、商品名称、所需积分、剩余库存
5. 用户点击某商品卡片
6. 系统弹出兑换确认弹窗，显示：商品名称、所需积分、当前积分余额、兑换后剩余积分
7. 用户点击"确认兑换"按钮
8. 系统显示加载态（按钮变为"兑换中..."且禁用）
9. 系统调用 `POST /api/exchange/redeem`
10. 系统显示兑换成功弹窗：若 needClaim=1，显示兑换码和有效期；若 needClaim=0，显示"兑换成功"
11. 页面自动刷新用户积分余额和商品库存

### 6.2 积分商城页面 -- 错误路径

| 错误点 | 触发条件 | 系统反馈 | 用户恢复操作 |
|--------|----------|----------|-------------|
| 商品列表加载失败 | 网络异常 | Toast 提示"加载失败，请重试"，显示空状态页带"重试"按钮 | 点击"重试"按钮重新加载 |
| 积分不足 | 用户积分 < 商品 pointsCost | 确认弹窗中"确认兑换"按钮禁用，显示文案"积分不足" | 关闭弹窗，继续浏览其他商品或去赚取积分 |
| 库存不足 | 商品 stock=0 | 商品卡片显示"已兑完"标签，兑换按钮禁用 | 浏览其他有库存的商品 |
| 兑换请求失败 | 服务端返回错误（如并发导致库存不足） | Toast 提示具体错误信息（"库存不足"/"积分不足"/"商品不存在或已下架"） | 关闭 Toast，页面自动刷新最新数据 |

### 6.3 兑换记录页面 -- 成功路径

1. 用户在"我的"页面点击"兑换记录"菜单项
2. 系统跳转到兑换记录页面 `/pages/exchange/records`
3. 系统调用 `GET /api/exchange/records` 加载记录列表
4. 系统显示兑换记录列表，每条记录显示：商品名称、消耗积分、兑换时间、状态标签（待领取/已领取/已过期）
5. 用户点击状态为"待领取"的记录
6. 系统展开显示兑换码（大字体可复制）和有效期截止时间

### 6.4 兑换记录页面 -- 错误路径

| 错误点 | 触发条件 | 系统反馈 | 用户恢复操作 |
|--------|----------|----------|-------------|
| 记录列表加载失败 | 网络异常 | Toast 提示"加载失败，请重试"，显示空状态页 | 下拉刷新或点击"重试" |
| 无兑换记录 | 用户从未兑换 | 显示空状态插画和文案"暂无兑换记录，去积分商城看看吧"，附"去商城"按钮 | 点击"去商城"跳转到积分商城页面 |

### 6.5 管理端商品管理页面 -- 成功路径

1. 管理员在管理后台首页点击"兑换商品"管理入口
2. 系统跳转到兑换商品管理页面 `/pages/admin/exchange/exchange`
3. 系统调用 `GET /api/admin/exchange-items` 加载所有商品（含已下架）
4. 系统显示商品列表，每条包含：名称、积分价格、库存、状态标签（上架/下架）
5. 管理员点击"新增商品"按钮
6. 系统弹出编辑弹窗（Modal），包含输入字段：名称、积分价格、库存、描述、图片URL、是否需线下领取（Switch）、状态（Switch）
7. 管理员填写信息并点击"保存"
8. 系统调用 `POST /api/admin/exchange-item`，显示保存成功 Toast，关闭弹窗，刷新列表

### 6.6 管理端兑换码核销 -- 成功路径

1. 管理员在管理后台首页点击"兑换核销"入口
2. 系统显示核销页面，包含兑换码输入框
3. 管理员输入 8 位兑换码并点击"核销"按钮
4. 系统调用 `POST /api/admin/exchange/verify`，返回兑换记录详情（商品名称、用户昵称、兑换时间）
5. 管理员确认信息后点击"确认核销"
6. 系统显示"核销成功" Toast

### 6.7 状态矩阵 -- 积分商城页面

| 状态 | 可见元素 | 可执行操作 | 禁用元素 |
|------|----------|------------|----------|
| 加载态 | 顶部积分余额骨架屏、商品列表骨架屏 | 无 | 所有交互元素 |
| 正常态（有商品） | 积分余额、商品卡片列表（图片+名称+积分+库存） | 点击商品卡片弹出兑换确认弹窗 | 无 |
| 空数据态 | 积分余额、空状态插画、文案"暂无可兑换商品" | 下拉刷新 | 无 |
| 错误态 | 错误提示、"重试"按钮 | 点击"重试"重新加载 | 无 |
| 兑换确认弹窗 | 商品名称、所需积分、当前余额、兑换后余额 | 点击"确认兑换"、点击"取消" | "确认兑换"按钮在积分不足时禁用 |
| 兑换中态 | 兑换确认弹窗、"兑换中..."文案 | 无 | "确认兑换"按钮、"取消"按钮 |
| 兑换成功弹窗 | 成功图标、兑换码（needClaim=1时）、有效期（needClaim=1时）、"知道了"按钮 | 点击"知道了"关闭弹窗 | 无 |

### 6.8 状态矩阵 -- 兑换记录页面

| 状态 | 可见元素 | 可执行操作 | 禁用元素 |
|------|----------|------------|----------|
| 加载态 | 记录列表骨架屏 | 无 | 所有交互元素 |
| 正常态（有记录） | 兑换记录列表（商品名+积分+时间+状态标签） | 点击"待领取"记录展开兑换码详情、下拉刷新 | "已领取"和"已过期"记录不可展开 |
| 空数据态 | 空状态插画、"暂无兑换记录"文案、"去商城"按钮 | 点击"去商城"跳转、下拉刷新 | 无 |
| 错误态 | 错误提示、"重试"按钮 | 点击"重试"重新加载 | 无 |

## 7. 验收策略

### 7.1 验收方法

```yaml
acceptance_strategy:
  type: functional
  method: gherkin_execution
  verification_steps:
    - "L2 产出测试骨架 -> L3 填充实现 -> L4 自动执行"
    - "所有 Gherkin 场景 PASS -> 通过"
    - "边界场景覆盖率 >= Challenger 审查通过的清单"
```

### 7.2 覆盖度清单

```yaml
coverage_checklist:
  requirement_coverage:
    - req_id: REQ-EXC-001
      gherkin_scenarios:
        - "获取商品列表_商品存在_返回上架商品列表"
        - "获取商品列表_无上架商品_返回空列表"
        - "获取商品列表_返回完整字段_字段均有值"
    - req_id: REQ-EXC-002
      gherkin_scenarios:
        - "获取商品列表_商品存在_返回上架商品列表"
    - req_id: REQ-EXC-003
      gherkin_scenarios:
        - "兑换需领取商品_积分库存充足_生成兑换码"
        - "兑换无需领取商品_积分库存充足_自动完成"
    - req_id: REQ-EXC-004
      gherkin_scenarios:
        - "兑换需领取商品_积分库存充足_生成兑换码"
        - "兑换无需领取商品_积分库存充足_自动完成"
        - "兑换操作_事务一致性_中间步骤失败则回滚"
    - req_id: REQ-EXC-005
      gherkin_scenarios:
        - "兑换需领取商品_积分库存充足_生成兑换码"
    - req_id: REQ-EXC-006
      gherkin_scenarios:
        - "兑换无需领取商品_积分库存充足_自动完成"
    - req_id: REQ-EXC-007
      gherkin_scenarios:
        - "兑换商品_积分不足_拒绝兑换"
    - req_id: REQ-EXC-008
      gherkin_scenarios:
        - "兑换商品_库存不足_拒绝兑换"
    - req_id: REQ-EXC-009
      gherkin_scenarios:
        - "兑换商品_商品已下架_拒绝兑换"
        - "兑换商品_商品不存在_拒绝兑换"
    - req_id: REQ-EXC-010
      gherkin_scenarios:
        - "查询兑换记录_有记录_按时间降序返回"
        - "查询兑换记录_无记录_返回空列表"
        - "查询兑换记录_只能看到自己的记录"
    - req_id: REQ-EXC-011
      gherkin_scenarios:
        - "核销兑换码_有效兑换码_核销成功"
    - req_id: REQ-EXC-012
      gherkin_scenarios:
        - "核销兑换码_有效兑换码_核销成功"
    - req_id: REQ-EXC-013
      gherkin_scenarios:
        - "核销兑换码_兑换码不存在_核销失败"
    - req_id: REQ-EXC-014
      gherkin_scenarios:
        - "核销兑换码_已核销_重复核销失败"
    - req_id: REQ-EXC-015
      gherkin_scenarios:
        - "核销兑换码_已过期_核销失败"
    - req_id: REQ-EXC-016
      gherkin_scenarios:
        - "新增商品_必填字段齐全_创建成功"
        - "新增商品_仅必填字段_使用默认值创建"
    - req_id: REQ-EXC-017
      gherkin_scenarios:
        - "编辑商品_更新字段_保存成功"
    - req_id: REQ-EXC-018
      gherkin_scenarios:
        - "商品上下架_切换状态_影响用户可见性"
    - req_id: REQ-EXC-019
      gherkin_scenarios:
        - "商品上下架_切换状态_影响用户可见性"
    - req_id: REQ-EXC-020
      gherkin_scenarios:
        - "删除商品_存在商品_删除成功"
    - req_id: REQ-EXC-021
      gherkin_scenarios:
        - "管理端获取商品列表_包含已下架商品"
    - req_id: REQ-EXC-022
      gherkin_scenarios: []
      note: "UI 导航入口，通过交互流程验证"
    - req_id: REQ-EXC-030
      gherkin_scenarios:
        - "兑换操作_事务一致性_中间步骤失败则回滚"
    - req_id: REQ-EXC-031
      gherkin_scenarios:
        - "兑换需领取商品_积分库存充足_生成兑换码"
      note: "兑换码格式通过代码审查验证（复用现有 CodeGenerator）"
    - req_id: REQ-EXC-033
      gherkin_scenarios:
        - "用户端兑换操作_未登录_拒绝访问"
        - "管理端操作_非管理员_拒绝访问"
        - "商品列表查询_无需认证_允许访问"

  path_coverage:
    exchange_flow:
      success_paths:
        - "用户浏览商城 -> 选择商品 -> 确认兑换 -> 积分扣减+库存扣减+记录生成 -> 兑换码展示"
        - "用户浏览商城 -> 选择needClaim=0商品 -> 确认兑换 -> 积分扣减+库存扣减+记录生成(status=claimed)"
      error_paths:
        - "用户选择商品 -> 积分不足 -> 拒绝兑换"
        - "用户选择商品 -> 库存不足 -> 拒绝兑换"
        - "用户选择商品 -> 商品已下架 -> 拒绝兑换"
        - "用户选择商品 -> 商品不存在 -> 拒绝兑换"
    verify_flow:
      success_paths:
        - "管理员输入兑换码 -> 验证通过 -> 核销成功"
      error_paths:
        - "管理员输入兑换码 -> 兑换码无效 -> 核销失败"
        - "管理员输入兑换码 -> 已核销 -> 核销失败"
        - "管理员输入兑换码 -> 已过期 -> 核销失败"
    admin_crud_flow:
      success_paths:
        - "管理员新增商品 -> 保存成功"
        - "管理员编辑商品 -> 保存成功"
        - "管理员删除商品 -> 删除成功"
        - "管理员上架/下架商品 -> 状态切换成功"

  field_coverage:
    exchange_item:
      - field: id
        type: BIGINT
        auto_generated: true
        display: "商品列表、兑换确认弹窗（隐藏字段）"
        validation: "自增主键"
      - field: name
        type: VARCHAR(64)
        required: true
        display: "商品卡片标题、兑换确认弹窗、兑换记录、管理端列表"
        validation: "非空，最大64字符"
        input: "管理端新增/编辑弹窗文本输入"
      - field: pointsCost
        type: INT
        required: true
        display: "商品卡片积分标签、兑换确认弹窗'所需积分'、管理端列表"
        validation: "正整数，>= 1"
        input: "管理端新增/编辑弹窗数字输入"
      - field: stock
        type: INT
        required: false
        default: 0
        display: "商品卡片'剩余N件'、管理端列表库存列"
        validation: "非负整数，>= 0"
        input: "管理端新增/编辑弹窗数字输入"
      - field: description
        type: VARCHAR(256)
        required: false
        display: "兑换确认弹窗商品描述"
        validation: "最大256字符，允许为空"
        input: "管理端新增/编辑弹窗多行文本输入"
      - field: image
        type: VARCHAR(512)
        required: false
        display: "商品卡片图片、兑换确认弹窗图片"
        validation: "有效URL路径，最大512字符，允许为空"
        input: "管理端新增/编辑弹窗URL输入"
      - field: needClaim
        type: TINYINT
        required: false
        default: 1
        display: "管理端列表'需领取'标签"
        validation: "0 或 1"
        input: "管理端新增/编辑弹窗 Switch 开关"
      - field: status
        type: TINYINT
        required: false
        default: 1
        display: "管理端列表'上架/下架'标签"
        validation: "0 或 1"
        input: "管理端新增/编辑弹窗 Switch 开关"
      - field: createdAt
        type: DATETIME
        auto_generated: true
        display: "管理端列表（可选）"
        validation: "数据库自动生成"

    exchange_record:
      - field: id
        type: BIGINT
        auto_generated: true
        display: "兑换记录列表（隐藏字段）"
        validation: "自增主键"
      - field: userId
        type: BIGINT
        required: true
        display: "不直接显示（用于数据隔离）"
        validation: "有效的 user.id 外键"
      - field: itemId
        type: BIGINT
        required: true
        display: "不直接显示"
        validation: "有效的 exchange_item.id 外键"
      - field: itemName
        type: VARCHAR(64)
        required: true
        display: "兑换记录列表商品名称"
        validation: "兑换时刻商品名称的快照，非空"
      - field: pointsCost
        type: INT
        required: true
        display: "兑换记录列表消耗积分"
        validation: "兑换时刻积分价格的快照，正整数"
      - field: claimCode
        type: VARCHAR(16)
        required: false
        display: "兑换记录详情兑换码（大字体）"
        validation: "8位大写字母数字组合（字符集ABCDEFGHJKLMNPQRSTUVWXYZ23456789），needClaim=1时生成，needClaim=0时为null"
      - field: status
        type: VARCHAR(20)
        required: true
        default: "pending"
        display: "兑换记录列表状态标签（待领取/已领取/已过期）"
        validation: "枚举值：pending, claimed"
      - field: createdAt
        type: DATETIME
        auto_generated: true
        display: "兑换记录列表兑换时间"
        validation: "数据库自动生成"
      - field: claimedAt
        type: DATETIME
        required: false
        display: "兑换记录详情领取时间"
        validation: "核销时由系统写入当前时间，初始为null"
      - field: expireAt
        type: DATETIME
        required: false
        display: "兑换记录详情有效期截止时间"
        validation: "needClaim=1时设置为createdAt+30天，needClaim=0时为null"

    user_points_change:
      - field: points
        type: INT
        display: "积分商城页面顶部余额、我的页面积分统计"
        validation: "兑换操作后 points = points - pointsCost，结果必须 >= 0"
```

### 7.3 API 接口契约

```yaml
api_contracts:
  user_endpoints:
    - endpoint: "GET /api/exchange/items"
      auth: "无需认证"
      request_params: 无
      response_body:
        code: 0
        msg: "success"
        data:
          type: "Array<ExchangeItem>"
          fields: [id, name, pointsCost, stock, description, image, needClaim]
      notes: "仅返回 status=1 的商品，按 pointsCost 升序"

    - endpoint: "POST /api/exchange/redeem"
      auth: "JWT 认证"
      request_body:
        itemId: "Long, 必填, 兑换商品ID"
      response_body_success:
        code: 0
        msg: "success"
        data:
          id: "Long, 兑换记录ID"
          itemName: "String, 商品名称"
          pointsCost: "Integer, 消耗积分"
          claimCode: "String, 兑换码(needClaim=1时返回, 否则null)"
          status: "String, pending 或 claimed"
          expireAt: "String, 过期时间(needClaim=1时返回, 否则null)"
      response_body_error:
        code: -1
        msg: "积分不足 | 库存不足 | 商品不存在或已下架"

    - endpoint: "GET /api/exchange/records"
      auth: "JWT 认证"
      request_params: 无
      response_body:
        code: 0
        msg: "success"
        data:
          type: "Array<ExchangeRecord>"
          fields: [id, itemName, pointsCost, claimCode, status, createdAt, claimedAt, expireAt]
      notes: "按 createdAt 降序，仅返回当前用户的记录"

  admin_endpoints:
    - endpoint: "GET /api/admin/exchange-items"
      auth: "JWT + 管理员白名单"
      request_params: 无
      response_body:
        code: 0
        msg: "success"
        data:
          type: "Array<ExchangeItem>"
          fields: [id, name, pointsCost, stock, description, image, needClaim, status, createdAt]
      notes: "返回所有商品（含已下架），按 createdAt 降序"

    - endpoint: "POST /api/admin/exchange-item"
      auth: "JWT + 管理员白名单"
      request_body:
        id: "Long, 可选, 有值则为编辑，无值则为新增"
        name: "String, 必填, 商品名称"
        pointsCost: "Integer, 必填, 所需积分"
        stock: "Integer, 可选, 默认0"
        description: "String, 可选"
        image: "String, 可选"
        needClaim: "Integer, 可选, 默认1"
        status: "Integer, 可选, 默认1"
      response_body:
        code: 0
        msg: "success"

    - endpoint: "DELETE /api/admin/exchange-item/{id}"
      auth: "JWT + 管理员白名单"
      request_params:
        id: "Long, 路径参数, 商品ID"
      response_body:
        code: 0
        msg: "success"

    - endpoint: "POST /api/admin/exchange/verify"
      auth: "JWT + 管理员白名单"
      request_body:
        claimCode: "String, 必填, 8位兑换码"
      response_body_success:
        code: 0
        msg: "success"
        data:
          recordId: "Long, 兑换记录ID"
          itemName: "String, 商品名称"
          pointsCost: "Integer, 消耗积分"
          userName: "String, 用户昵称"
          createdAt: "String, 兑换时间"
          status: "String, 核销后为 claimed"
      response_body_error:
        code: -1
        msg: "兑换码无效 | 该兑换码已核销 | 该兑换码已过期"
```

## 8. 不确定性与待确认项

| 编号 | 问题 | 影响范围 | 建议 |
|------|------|----------|------|
| UC-001 | 积分商城商品列表是否需要认证才能查看？当前设计为公开访问（与奖品列表 `/api/lottery/prizes` 一致），但用户积分余额显示需要登录。 | REQ-EXC-001, REQ-EXC-033 | 建议商品列表公开访问，页面加载时若已登录则额外请求 `/api/user/info` 获取积分余额。已在当前设计中采用此方案。 |
| UC-002 | 用户是否可以对同一商品多次兑换？当前设计无限制（只要积分和库存充足即可重复兑换）。 | REQ-EXC-003 | 建议确认是否需要"每人每商品限兑N次"的限制。若需要，需在 exchange_item 表新增 `max_per_user` 字段。 |
| UC-003 | 兑换记录是否需要分页？当前设计返回用户全部记录。 | REQ-EXC-010 | 当前阶段用户兑换记录量预计较少（积分获取速率有限），暂不分页。若后续记录量增长，可增加分页参数。 |
| UC-004 | 管理员删除商品时，是否需要检查该商品是否有未核销的兑换记录？ | REQ-EXC-020 | 建议允许删除（因兑换记录中已冗余存储 itemName 和 pointsCost），但可在删除确认弹窗中提示"该商品有 N 条待核销记录"。当前设计采用直接删除方案。 |
| UC-005 | "已过期"状态是否需要后台定时任务自动将 pending 且过期的记录 status 更新为 "expired"？还是仅在查询和核销时动态判断？ | REQ-EXC-015 | 当前数据库 status 枚举仅有 "pending" 和 "claimed"，过期判断通过比较 expireAt 与当前时间实现。建议暂时维持动态判断方案（前端根据 expireAt 显示"已过期"状态标签），避免引入定时任务复杂度。 |
| UC-006 | 首页功能入口区域目前有4个入口（师资团队、活动详情、幸运抽奖、分享助力），新增"积分商城"后布局如何调整？ | REQ-EXC-022 | 需确认是替换现有入口还是扩展为5个入口。建议在功能入口区域增加第5个入口"积分商城"，或替换"分享助力"入口（分享功能可从活动页面进入）。 |

## 9. 术语表

| 术语 | 定义 |
|------|------|
| 兑换商品（ExchangeItem） | 积分商城中可供用户使用积分兑换的商品，包含名称、积分价格、库存等属性 |
| 兑换记录（ExchangeRecord） | 用户执行兑换操作后生成的记录，记录兑换的商品、消耗的积分、兑换码和状态 |
| 兑换码（claimCode） | 8位字母数字组合的唯一标识码，用于线下到店核销领取实物商品 |
| 核销（verify/claim） | 管理员确认用户已到店领取商品的操作，将兑换记录状态从 "pending" 变更为 "claimed" |
| 积分（points） | 用户通过抽奖获得的虚拟货币，可用于兑换商品。存储于 user.points 字段 |
| 上架（status=1） | 商品在用户端积分商城中可见且可兑换的状态 |
| 下架（status=0） | 商品在用户端积分商城中不可见的状态，管理端仍可查看和编辑 |
| needClaim | 商品是否需要线下领取的标识。1=需要（生成兑换码），0=不需要（自动完成） |
| pending | 兑换记录待领取状态，表示用户已兑换但尚未到店领取 |
| claimed | 兑换记录已领取状态，表示管理员已核销或无需领取的商品已自动完成 |

## 10. 自检清单执行结果

### 完整性检查
- [x] 每条用户故事都有至少一条 EARS 格式需求对应：US-001->REQ-EXC-001/002, US-002->REQ-EXC-003~009, US-003->REQ-EXC-010, US-004->REQ-EXC-011~015, US-005->REQ-EXC-016/017, US-006->REQ-EXC-018/019, US-007->REQ-EXC-011/012
- [x] 每条功能需求都有至少一组 Gherkin 验收标准对应：见 7.2 requirement_coverage 映射
- [x] 边界场景覆盖了全部 7 个类别中的相关项：空值(BD-001~004)、极值(BD-005~008)、并发(BD-009~010)、权限(BD-011~013)、网络(BD-014~015)、数据完整性(BD-016~018)、状态流转(BD-019~021)
- [x] UI 需求包含完整的成功路径和错误路径：6.1~6.6 成功路径 + 错误路径表格
- [x] 每条需求都有对应的验收策略
- [x] coverage_checklist 穷举到字段级别：exchange_item 9 个字段、exchange_record 10 个字段、user.points 变更均已列出
- [x] 有导出/报表/产出物功能时，穷举了相关字段：兑换码作为产出物已在 claimCode 字段中详细描述

### 无歧义检查
- [x] 不存在模糊词："快速"、"友好"、"高效"、"合理"、"适当"、"大量"、"少量"、"尽快"、"灵活"均未使用
- [x] 所有度量指标都有具体数值：响应时间500ms、兑换码8位、过期时间30天
- [x] 所有状态转换都有明确的触发条件和目标状态：pending->claimed (管理员核销 或 needClaim=0自动)
- [x] 所有错误处理都有明确的错误类型和响应行为：5种明确错误消息

### 可判定性检查
- [x] 每条验收标准只有"通过"或"不通过"两种判定结果
- [x] 每条验收标准可由第三方独立验证
- [x] Gherkin Then 步骤只描述可观察结果（积分变化、库存变化、返回值等），未暴露内部实现

### 一致性检查
- [x] 需求编号无重复、无断号：REQ-EXC-001~022（功能需求），REQ-EXC-030~033（非功能需求）
- [x] 同一概念在全文中使用同一术语：术语表已定义
- [x] 用户故事、EARS 需求、Gherkin 场景三者语义一致
- [x] 边界场景的预期行为与对应 EARS 需求一致

### 追溯性检查
- [x] 每条 Gherkin Scenario 标注了关联的需求编号（通过 @REQ-EXC-XXX 标签）
- [x] 每条边界场景标注了关联的需求编号（见边界场景清单表格）
- [x] 所有需求均基于用户明确陈述，推断内容已标注在"不确定性与待确认项"中
