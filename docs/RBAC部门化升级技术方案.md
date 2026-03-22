# RBAC 部门化升级技术方案

## 1. 背景

当前系统的权限模型仍停留在“两级角色 + 个人数据域”阶段：

- 角色只有 `SUPERADMIN` 和 `OPERATOR`
- 用户没有部门归属
- 无人机、任务、报警规则、分析数据等资源的访问范围主要依赖 `pilotName / ownerName` 这类姓名字段
- 超级管理员可以管理所有账号，但不存在“部门负责人”这一层
- 执行者与执行者之间无法形成“部门内共享”

这套模型已经无法满足当前业务诉求：

- 每个用户都需要归属到某个部门
- 每个部门需要区分“部门负责人”和“执行者”
- 部门负责人需要在本部门内配置资源并分享给部门成员
- 部门成员之间需要共享本部门的无人机资源、任务执行数据、报警模板/规则
- 超级管理员需要统一管理部门、负责人和执行者

## 2. 当前现状总结

### 2.1 角色模型

当前后端角色枚举仅包含：

- `SUPERADMIN`
- `OPERATOR`

对应代码：

- `codes/uav-backend/src/main/java/com/example/uavbackend/auth/UserRole.java`

### 2.2 用户模型

当前 `users` 表和 `User` 实体只有：

- 用户名
- 密码
- 姓名
- 角色
- 状态

没有：

- `department_id`
- `department_name`
- 部门内职位类型

### 2.3 数据域控制

当前 RBAC 数据域主要依赖用户姓名进行过滤：

- 无人机按 `pilotName`
- 任务按 `pilotName`
- 分析数据按 `ownerName / operatorName`
- 告警记录按可访问任务反推

这意味着：

- 权限控制依赖姓名，不够稳定
- 资源归属仍是“个人归属”，不是“部门归属”
- 同部门共享无法自然表达

### 2.4 用户管理入口

当前前端人员管理页只支持：

- 新增用户
- 指定 `superadmin / operator`
- 重置密码
- 删除用户

不支持：

- 部门管理
- 部门负责人管理
- 执行者按部门管理
- 部门内资源归属展示

## 3. 目标模型

### 3.1 角色体系

升级后的角色体系为：

- `SUPERADMIN`
  - 全局权限
  - 可创建部门
  - 可创建任意部门负责人和执行者
  - 可查看全部资源与数据
- `DEPT_LEAD`
  - 归属某个部门
  - 可管理本部门执行者
  - 可配置本部门无人机、报警模板、报警规则
  - 可查看本部门全部任务与分析数据
- `EXECUTOR`
  - 归属某个部门
  - 使用本部门共享资源执行任务
  - 可查看本部门共享任务数据
  - 仅可处理本部门待处理告警记录

### 3.2 部门体系

新增 `departments` 实体，每个部门包含：

- `id`
- `dept_code`
- `dept_name`
- `description`
- `status`

### 3.3 资源共享规则

#### 无人机

- 无人机归属部门
- 由超级管理员或部门负责人配置
- 本部门成员均可查看和使用

#### 报警模板 / 报警规则

- 模板和普通规则均归属部门
- 由超级管理员或部门负责人配置
- 本部门成员可在任务创建时使用本部门规则
- 执行者不可修改模板/规则

#### 任务与执行数据

- 任务归属部门
- 任务执行结果、分析记录、告警记录均归属任务所属部门
- 本部门成员共享查看
- 超级管理员查看全量

## 4. 数据模型改造

### 4.1 新增表

新增 `departments`：

- `id`
- `dept_code`
- `dept_name`
- `description`
- `status`
- `created_at`
- `updated_at`

### 4.2 users 表新增字段

新增：

- `department_id`
- `department_name`

说明：

- `SUPERADMIN` 可为空
- `DEPT_LEAD / EXECUTOR` 必须存在部门归属

### 4.3 资源表新增部门归属字段

以下表增加：

- `uav_devices.department_id`
- `uav_devices.department_name`
- `uav_devices.owner_username`
- `alert_rule.department_id`
- `alert_rule.department_name`
- `alert_rule.created_by`
- `missions.department_id`
- `missions.department_name`
- `missions.pilot_username`
- `mission_data_record.department_id`
- `mission_data_record.department_name`
- `task_executions.department_id`
- `task_executions.department_name`

其中：

- `owner_username / created_by / pilot_username` 用于稳定记录创建人
- `department_id / department_name` 用于快速做部门数据域过滤

## 5. 权限规则

### 5.1 用户管理

#### 超级管理员

- 可查看全部部门
- 可新增部门
- 可编辑部门
- 可删除空部门
- 可新增部门负责人
- 可新增部门执行者
- 可重置任何非自身超级管理员密码
- 可查看全部用户

#### 部门负责人

- 只能查看本部门
- 只能查看本部门成员
- 只能新增/编辑/删除本部门执行者
- 不能创建超级管理员
- 不能创建其他部门负责人
- 不能跨部门操作

#### 执行者

- 无用户管理权限

### 5.2 资源管理

#### 无人机管理

- `SUPERADMIN` 查看全部
- `DEPT_LEAD / EXECUTOR` 查看本部门无人机
- `DEPT_LEAD` 可接入本部门无人机
- `EXECUTOR` 默认只读或仅在既有页面保留使用权限，不开放资源配置

#### 报警模板 / 规则管理

- `SUPERADMIN` 可查看全部模板和规则
- `DEPT_LEAD` 可管理本部门模板和规则
- `EXECUTOR` 仅查看本部门待处理告警记录

### 5.3 任务与分析

- `SUPERADMIN` 查看全部任务与分析
- `DEPT_LEAD / EXECUTOR` 查看本部门任务、分析、复盘、对比数据

## 6. 后端改造方案

### 6.1 统一 AccessScope

将当前 `AccessScope` 升级为：

- 是否超管
- 当前用户名
- 当前姓名
- 当前角色
- 当前部门 ID
- 当前部门名称

并补充：

- `isDeptLead()`
- `isExecutor()`
- `belongsToDepartment(Long departmentId)`

### 6.2 部门服务

新增：

- `Department`
- `DepartmentMapper`
- `DepartmentService`
- `DepartmentController`

接口建议：

- `GET /api/departments`
- `POST /api/departments`
- `PUT /api/departments/{id}`
- `DELETE /api/departments/{id}`

### 6.3 用户服务升级

新增能力：

- 用户按部门查询
- 创建用户时指定部门与角色
- 部门负责人创建执行者
- 部门负责人只能操作本部门执行者

### 6.4 资源服务升级

#### FleetService

- 注册无人机时写入部门归属
- 列表和统计按部门过滤
- 任务选机按部门共享设备

#### AlertService

- 模板和规则写入部门归属
- `listTemplates / listRules / listAssignableRules` 按部门过滤
- 执行者仅查看本部门待处理报警

#### MissionService / AnalyticsService

- 任务创建写入部门归属
- 列表、详情、复盘、分析按部门过滤
- 聚合数据、历史执行记录落库时带部门信息

## 7. 前端改造方案

### 7.1 登录态扩展

前端 `currentUser` 增加：

- `departmentId`
- `departmentName`
- `role`

### 7.2 菜单与页面

- 将“人员管理”升级为“部门与成员管理”
- `SUPERADMIN` 可见：
  - 部门管理
  - 成员管理
- `DEPT_LEAD` 可见：
  - 本部门成员管理
- `EXECUTOR` 不可见成员管理入口

### 7.3 页面行为调整

- 无人机管理显示本部门共享设备
- 任务指挥选择器只列出本部门成员和无人机
- 报警规则页：
  - 超管：全局视图
  - 部门负责人：本部门模板/规则管理
  - 执行者：本部门报警记录收件箱
- 数据分析默认展示本部门数据

## 8. 测试数据方案

开发完成后，生成一套可直接登录验证的测试数据：

### 8.1 测试部门

- 森林巡查部
- 空气监测部
- 电网巡检部

### 8.2 测试账号

- 1 个超级管理员
- 每个部门 1 个负责人
- 每个部门 2 个执行者

### 8.3 测试资源

- 每个部门至少 2 架无人机
- 每个部门至少 2 个模板
- 每个部门至少 2 条普通规则
- 每个部门至少 2 条任务和对应分析数据

### 8.4 文档输出

将以下内容写入：

- `./docs/账号密码.md`

包含：

- 账号
- 密码
- 角色
- 所属部门
- 可验证资源示例

## 9. 实施顺序

1. 新增部门表与字段迁移
2. 升级用户角色与登录态
3. 升级 AccessScope 和后端数据域
4. 改造用户管理与部门管理接口
5. 改造无人机、报警规则、任务、分析模块
6. 改造前端人员管理入口与页面
7. 回灌测试数据与账号文档
8. 完整验证部门共享与权限边界

## 10. 验收标准

- 超级管理员可新增部门、负责人、执行者
- 部门负责人可管理本部门执行者
- 执行者不可管理用户
- 同部门成员共享无人机资源
- 同部门成员共享报警模板/规则使用权限
- 同部门成员共享任务与分析数据
- 不同部门之间数据隔离
- `./docs/账号密码.md` 中提供完整测试账号与资源说明
