# RBAC 数据域完善技术方案

## 1. 背景

当前系统已有两类角色：

- `SUPERADMIN`：超级管理员，具备全量访问能力
- `OPERATOR`：普通执行者，理论上应只能访问与自己相关的业务数据

现阶段系统的角色入口控制并不完整，更多停留在前端页面层，缺少后端统一的数据域收口，因此仍存在执行者通过接口拿到全量数据的风险。

## 2. 当前现状

### 2.1 已具备能力

- 登录后可识别当前用户角色
- 超级管理员具备全量配置和管理能力
- 前端“无人机管理”页面已对执行者做了一层本地过滤，仅展示 `pilotName === 当前用户名对应姓名` 的无人机

### 2.2 主要问题

#### 无人机管理

- 后端 `/api/fleet`、`/api/fleet/available`、`/api/fleet/summary` 仍按全量数据返回
- 执行者如果直接调用接口，仍可看到不属于自己的无人机

#### 任务指挥

- 后端 `/api/missions`、`/api/missions/{code}` 默认返回全量任务
- 前端任务指挥页未按执行者所属任务做过滤
- 执行者在创建任务时仍可感知其他执行者、其他无人机、全量报警规则

#### 数据分析

- `/api/analytics/definitions`、`/api/analytics/task-executions`、`/api/analytics/data`、`/api/analytics/compare`、`/api/analytics/replay`、`/api/analytics/timeseries` 均未按当前登录人做数据域控制
- 执行者可读取其他人员任务的分析数据与复盘数据

#### 报警规则管理

- 当前页面仅对超级管理员开放
- 后端 `/api/alerts/rules`、`/api/alerts/records` 未区分角色
- 业务预期是：执行者不管理规则模板与普通规则，只查看“与自己相关且待处理”的报警记录

### 2.3 数据模型限制

当前多个业务表主要使用 `pilotName`、`ownerName` 作为归属字段，而不是稳定的 `username`：

- `uav_device.pilot_name`
- `missions.pilot_name`
- `monitoring_task.owner_name`
- `mission_data_record.pilot_name / operator_name`
- `task_executions.owner_name`

这意味着本轮 RBAC 落地时，需要基于“当前登录用户的姓名”做归属过滤。该方案可满足当前业务，但中长期仍建议补充 `pilot_username / owner_username` 作为稳定外键。

## 3. 改进目标

### 3.1 超级管理员

- 保持 `full access`
- 继续访问全量无人机、任务、分析、报警配置与报警记录

### 3.2 执行者

- 无人机管理：只看自己负责的无人机
- 任务指挥：只看自己负责的任务
- 数据分析：只看自己任务产生的分析数据和复盘数据
- 报警规则管理：不看模板和规则配置，只看与自己任务相关、且待自己处理的报警记录

## 4. 设计方案

## 4.1 统一访问上下文

新增统一的登录用户上下文服务，用于在后端服务层识别：

- 当前用户名
- 当前姓名
- 当前角色
- 是否超级管理员

建议命名：`AccessScopeService`

输出统一的数据域对象：

- `isSuperAdmin`
- `username`
- `displayName`

## 4.2 后端数据域策略

### 无人机管理

- `SUPERADMIN`：返回全量
- `OPERATOR`：仅返回 `pilotName = 当前用户姓名` 的无人机

覆盖接口：

- `/api/fleet/summary`
- `/api/fleet`
- `/api/fleet/available`
- `/api/fleet` 新增接入时，若当前为执行者，则责任人只能是自己

### 任务指挥

- `SUPERADMIN`：返回全量
- `OPERATOR`：仅返回 `pilotName = 当前用户姓名` 的任务

覆盖接口：

- `/api/missions`
- `/api/missions/{missionCode}`
- `/api/missions/{missionCode}/interrupt`
- `/api/missions` 创建任务时，若当前为执行者，则 `pilotUsername` 强制为自己

### 数据分析

- `SUPERADMIN`：返回全量
- `OPERATOR`：仅允许访问自己任务的数据

覆盖接口：

- `/api/analytics/task-executions`
- `/api/analytics/data`
- `/api/analytics/compare`
- `/api/analytics/replay`
- `/api/analytics/timeseries`

实现方式：

- 先根据 `pilotName / ownerName` 限制查询范围
- 若按 `missionCode` 查询，则先校验任务归属，再返回数据

### 报警记录

- `SUPERADMIN`：继续查看全量规则与全量报警记录
- `OPERATOR`：
  - `/api/alerts/rules` 返回空列表或只用于前端兼容的空结果
  - `/api/alerts/records` 仅返回“属于自己任务”的报警记录
  - 默认进一步限制为 `processed = false`
  - `/api/alerts/records/{id}/process` 只能处理自己名下任务的报警

记录归属判定方式：

- 通过 `alert_record.mission_code -> missions.mission_code -> missions.pilot_name`

## 4.3 前端页面调整

### 路由层

- 超级管理员：保留全部原有入口
- 执行者：新增“报警记录”入口，复用报警规则管理页面，但页面改为只读记录视图

### 无人机管理

- 若当前为执行者：
  - 接入无人机时默认责任人为自己
  - 不展示其他操作员选项

### 任务指挥

- 若当前为执行者：
  - 任务列表仅显示自己的任务
  - 责任人固定为自己
  - 可选无人机仅显示自己的在线无人机
  - 报警规则下拉不展示规则配置管理含义，只作为“任务保护规则”选择

### 数据分析

- 若当前为执行者：
  - 所有任务候选、对比候选、复盘候选仅来自自己的任务
  - 导出结果仅导出自己的任务数据

### 报警规则管理

- 超级管理员：维持当前模板/普通规则双模式
- 执行者：
  - 页面切换为“待处理报警记录”
  - 不展示模板、规则、创建、编辑、删除入口
  - 支持按自己的任务多选筛选

## 5. 实施顺序

1. 新增统一访问上下文服务
2. 后端统一补数据域过滤
3. 补关键写操作的归属校验
4. 前端按角色收入口、表单和只读视图
5. 用 `superadmin` 和普通执行者双账号联调验证

## 6. 验证重点

### 超级管理员

- 可查看全量无人机、任务、分析、规则、报警记录

### 执行者

- 无法看见其他执行者的无人机
- 无法看见其他执行者的任务
- 无法读取其他执行者任务的分析数据/复盘数据
- 报警页面只显示与自己任务相关、待处理的报警记录
- 无法新增、编辑、删除报警规则模板或普通规则

## 7. 后续建议

本轮以最小侵入方式完成 RBAC 数据域控制。后续建议继续补强：

- 在 `missions`、`uav_device`、`monitoring_task`、`task_executions`、`mission_data_record` 等表中引入 `owner_username / pilot_username`
- 将“姓名过滤”迁移为“用户名过滤”
- 将角色与数据域进一步抽象成策略层，支持未来新增角色（如调度员、分析员、值班人员）
