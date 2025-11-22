# 无人机环境监测平台 API 接口文档

本文档依据 `/codes/uav-frontend/src` 的前端代码整理，覆盖平台正常的数据拉取 HTTP 接口以及实时 WebSocket 接口。所有接口默认以 `/api` 为前缀，除鉴权外均要求在请求头中携带 `Authorization: Bearer <token>`。示例中的字段名与前端类型保持一致（参见 `src/data/mock.ts`）。

## 通用约定

- **Base URL**：`https://{domain}/api`，所有示例已省略域名。
- **身份凭证**：除登录外的 HTTP 接口均需 `Authorization: Bearer <token>`。
- **默认请求头**：`Content-Type: application/json`, `Accept: application/json`。
- **默认响应头**：`Content-Type: application/json; charset=utf-8`，`X-Request-Id`（便于排查），`Cache-Control: no-store`（鉴权接口）。
- **分页 Query 参数**：`page`（从 1 开始）、`pageSize`（默认 20）。
- **时间格式**：ISO8601（UTC），如 `2024-06-18T08:23:00Z`。

## 接口分类总览

| 分类 | HTTP 接口 | WebSocket | 说明 |
| --- | --- | --- | --- |
| 用户与鉴权 | 登录、登出、获取当前用户 | — | 登录页、鉴权守卫 `AuthContext` 需要 |
| 机队中心 | 机队总览、机队列表、无人机增删改、空闲无人机查询 | UAV 状态流 | `pages/FleetCenter.tsx`、Dashboard 概览使用 |
| 任务指挥 | 任务类型、任务列表与详情、创建/更新/中断任务 | 任务事件流 | `pages/MissionCommander.tsx` 及地图航线需要 |
| 实时监测 | 执行中监测任务、任务指标/规则增删改 | 指标与视频流 | `pages/Monitoring.tsx` 视频/指标/规则维护 |
| AI 数据分析 | 任务执行历史查询、分析图配置 | — | `pages/DataAnalytics.tsx` 盈利用 `analyticsDefinitions` 和 `taskExecutions` |

---

## 用户与鉴权

### `POST /api/auth/login`
- 用途：登录页提交账号密码，返回访问令牌及用户信息。
- 请求头：`Content-Type: application/json`, `Accept: application/json`。
- 请求体示例：
```json
{
  "username": "superadmin",
  "password": "123456"
}
```
- 响应头：`Set-Cookie`（如需要刷新 token）、`Cache-Control: no-store`、`X-Request-Id`。
- 响应体示例：
```json
{
  "token": "jwt-token",
  "user": {
    "username": "superadmin",
    "name": "超级管理员",
    "role": "superadmin"
  }
}
```
- 错误：401（账号或密码错误）、423（账号锁定）。

### `GET /api/auth/profile`
- 用途：`AuthContext` 初始化时拉取当前登录用户。
- 请求头：`Authorization: Bearer <token>`。
- 响应头：`Cache-Control: no-store`、`X-Request-Id`。
- 响应体：同 `user` 结构。

### `POST /api/auth/logout`
- 用途：顶部“退出”按钮销毁会话。
- 请求头：`Authorization: Bearer <token>`。
- 请求体：空。
- 响应：`204 No Content`，包含 `X-Request-Id`。

---

## Dashboard/机队中心

### `GET /api/fleet/summary`
- 用途：仪表盘“今日运行概览”四个指标（online、warning、alerts、avgRtt）。
- 请求头：`Authorization`、`Accept: application/json`。
- 响应头：`Cache-Control: max-age=30`（可短期缓存）、`X-Request-Id`。
- 响应体：
```json
{
  "online": 42,
  "warning": 4,
  "alerts": 2,
  "avgRtt": 130
}
```

### `GET /api/fleet`
- 用途：机队列表；支持角色过滤。
- 请求头：`Authorization`、`Accept: application/json`。
- Query：`status`（online|warning|critical，可多值），`pilot`（仅返回该驾驶员可见的无人机），`page`/`pageSize`。
- 响应头：`X-Total-Count`（列表总数）、`X-Request-Id`。
- 响应体：
```json
{
  "items": [
    {
      "id": "UAV-21",
      "model": "多旋翼 · PX4 1.13",
      "mission": "林业健康-江北",
      "pilot": "张三",
      "battery": 78,
      "linkQuality": "优",
      "rtt": 120,
      "status": "online",
      "location": { "lat": 31.27, "lng": 121.46 },
      "lastHeartbeat": "2024-06-18T08:23:00Z"
    }
  ],
  "total": 120
}
```

### `POST /api/fleet`
- 用途：“接入无人机”表单提交；返回创建的无人机。
- 请求头：`Authorization`、`Content-Type: application/json`。
- 请求体字段：`id?`, `model`, `pilot`, `status`, `connection`。无需传递任务 ID、电量、连接质量或 RTT，这些由后台在握手成功后通过遥测实时写入。
- 响应：新建的 `FleetItem`。
- `connection` 结构建议包含：
  - `endpoint`: `rtsp://`、`udp://` 或专有指挥服务器地址（如 `p2p://uav-21`），用于后端主动连接无人机。
  - `protocol`: `mavlink` / `http` / `rtk` 等，用于选择对应的适配器。
  - `secret` 或 `pairingCode`: 无人机侧的配对口令，后端使用它完成 mutual auth。
  - `telemetryTopics`: 可选，表示无人机会推送的主题或传感器列表，便于后端订阅。

- 响应头：`Location: /api/fleet/{uavId}`、`X-Request-Id`。

> 说明：无人机的电量、链路、RTT、任务绑定信息均由后端根据遥测/任务调度自动写入，如非必要无需提供手工修改接口。

### `GET /api/fleet/available`
- 用途：任务创建弹窗中“执行无人机”下拉；仅返回在线、未被占用的无人机。
- Query：`missionType`, `excludeMissionIds[]`。
- 响应：`FleetItem[]` + `assignedMissionId?`。
- 请求/响应头：同 `GET /api/fleet`。

### 无人机接入/登记流程建议
1. **前端收集连接信息**：`/api/fleet` 表单除基础信息外仅收集无人机接入点（如 4G 终端 IP+端口、RTSP/RTMP 流地址、专有路由编号）以及配对口令，其他实时指标由后端心跳生成。
2. **服务端登记**：后端收到 `POST /api/fleet` 后写入无人机基础资料，并创建一个“连接任务”记录，状态为 `pending_connect`。
3. **建立链路**：后端适配器根据 `connection.protocol` 发起握手（例如对 MAVLink PX4 通过 UDP Dial、对 RTSP/SRT 建立媒体通道）。成功后：
   - 在设备表中写入连接 `sessionId`、`rangeKm` 初始值等。
   - 通知无人机推送遥测到 `ws/fleet-status`，或由后端根据链路定期采集。
4. **同步状态**：握手完成后，由后端直接更新设备表中的 `status=online` 并通过 `ws/fleet-status` 推送 `uav.connected` 事件；失败则写入 `status=critical` 与失败原因，供前端提示。
5. **释放/注销**：后续若需要解绑无人机，可提供 `DELETE /api/fleet/{uavId}` 或 `POST /api/fleet/{uavId}/disconnect`，后端断开底层连接并更新状态。

通过这种“前端提供接入参数 → 后端登记并主动建链 → WebSocket 心跳同步”的流程，可以确保每架无人机在平台上有可追踪的连接生命周期。

--- 

## 任务指挥中心

### `GET /api/mission-types`
- 用途：任务类型选项与推荐指标，直接映射 `missionTypeDefinitions`。
- 请求头：`Authorization`。
- 响应：
```json
{
  "items": [
    {
      "name": "森林火情巡查",
      "description": "热成像 + 烟雾识别，关注早期火点与气象",
      "recommendedSensors": ["热成像", "可见光", "气象套件"],
      "metrics": [
        { "key": "surfaceTemp", "label": "地表温度", "unit": "°C", "desc": "热成像最高温度/范围" }
      ]
    }
  ]
}
```

### `GET /api/missions`
- 用途：任务列表（`MissionCommander.tsx` 左侧列表、航线图、多选下拉）。
- Query：`status`（可多选执行中/排队/完成/异常中止）、`keyword`, `pilot`, `missionType`。
- 响应：`Mission[]`，包含 `route`, `milestones`, `metrics`, `assignedUavs`。
- 请求/响应头：同 `GET /api/fleet`。

### `GET /api/missions/{missionId}`
- 用途：Drawer 中需要的细节，返回 `Mission` 及实时进展（`progressHistory`, `alerts`）。
- 请求头：`Authorization`。
- 响应头：`X-Request-Id`。

### `POST /api/missions`
- 用途：“新增任务”表单提交。
- 请求头：`Authorization`、`Content-Type: application/json`。
- 请求体字段：`name`, `missionType`, `pilot`, `priority`, `status`, `route`, `progress?`, `assignedUavs?[]`, `metrics?[]`。
  - `route`：数组形式的航线 `[ [lat, lng], ... ]`，前端在创建时通过地图选择，至少 3 个点。
  - 服务端在写入前需校验：
    1. 航线闭合：首尾坐标距离 < 10m，或自动闭合失败则报错（`MISSION.ROUTE_NOT_CLOSED`）。
    2. 航线起点距离无人机当前位置 <= 剩余续航距离的 50%。
    3. 航线整体里程（起点出发、闭合回到起点）不超过无人机剩余续航的 80%。
- 响应：创建后的 `Mission`（含服务端生成的 `id`、`route`）。
- 响应头：`Location: /api/missions/{missionId}`、`X-Request-Id`。

### `PATCH /api/missions/{missionId}`
- 用途：更新进度、优先级、已分配无人机等。
- 示例：`{"progress": 85, "assignedUavs": ["UAV-09"]}`。
- 请求头：`Authorization`、`Content-Type: application/json`。
- 响应头：`X-Request-Id`。

### `POST /api/missions/{missionId}/interrupt`
- 用途：Drawer “中断任务”操作，将任务状态置为 `异常中止` 并释放无人机。
- 响应：最新 `Mission`。需返回是否已通知执行无人机。
- 请求头：`Authorization`。
- 响应头：`X-Request-Id`。

---

## 实时监测中心

### `GET /api/monitoring/tasks`
- 用途：右栏实时监测任务列表（仅 `status=执行中`）。
- Query：`status`, `missionType`, `owner`。
- 响应：任务概要数组（不含大字段的视频/趋势）。
- 请求头：`Authorization`。
- 响应头：`X-Request-Id`。

### `GET /api/monitoring/tasks/{taskId}`
- 用途：点击任务后加载详情，返回 `MonitoringTask` 全量信息：
  - `data`: 指标快照。
  - `metricTrend`: `Record<string, MetricSeriesPoint[]>`，用于趋势/统计。
  - `executionTimeline`: 执行步骤。
  - `videoStream`: 如果使用 WebSocket 视频，则返回 `streamId`；否则返回 `videoUrl`。
  - `rules`: 当前报警规则。
- 请求头：`Authorization`。
- 响应头：`X-Request-Id`。

### `POST /api/monitoring/tasks/{taskId}/rules`
- 用途：“新增规则”提交。
- 请求体：`name`, `metric`, `threshold`, `level`。
- 响应：新建 `Rule`。
- 请求头：`Authorization`, `Content-Type: application/json`。
- 响应头：`Location: /api/monitoring/tasks/{taskId}/rules/{ruleId}`, `X-Request-Id`。

### `PUT /api/monitoring/tasks/{taskId}/rules/{ruleId}`
- 用途：规则编辑。
- 请求体：与 `Rule` 相同。
- 请求头：`Authorization`, `Content-Type: application/json`。
- 响应头：`X-Request-Id`。

### `DELETE /api/monitoring/tasks/{taskId}/rules/{ruleId}`
- 用途：规则删除。
- 请求头：`Authorization`。
- 响应：`204 No Content` + `X-Request-Id`。

### `POST /api/monitoring/tasks/{taskId}/ack-alert`
- 可选：当 WebSocket 推送告警后前端确认，便于后台记录。
- 请求头：`Authorization`。

---

## AI 数据分析中心

### `GET /api/analytics/definitions`
- 用途：`analyticsDefinitions` 映射的图表配置，可按 `missionType` 过滤。
- 请求头：`Authorization`。
- 响应：
```json
{
  "missionType": "森林火情巡查",
  "definitions": [
    {
      "title": "地表温度",
      "description": "最高/最低/平均地表温度",
      "series": [
        { "name": "最高地表温度", "metric": "地表温度", "field": "max" }
      ]
    }
  ]
}
```

### `GET /api/analytics/task-executions`
- 用途：折线图与表格数据源，对应 `taskExecutions`。
- Query：`missionType`（必选）、`from`, `to`, `limit`（默认 50）、`owner`。
- 响应：`TaskExecution[]`。
- 说明：`metrics` 中的对象包含 `max|min|avg|count|value` 等字段，前端按定义动态读取。
- 请求/响应头：同 `GET /api/fleet`（含 `X-Total-Count`）。

---

## WebSocket 接口

### `GET /ws/fleet-status`
- 用途：实时推送所有在线无人机心跳，供机队中心、Dashboard 展示链路与电量变更。
- 鉴权：同 HTTP，使用 `Sec-WebSocket-Protocol: bearer,<token>` 或 `Authorization` query 参数。
- 推送消息：
```json
{
  "type": "uav.telemetry",
  "uavId": "UAV-21",
  "timestamp": "2024-06-18T08:23:00Z",
  "status": "online",
  "battery": 76,
  "rtt": 110,
  "linkQuality": "优",
  "missionId": "M-20240610",
  "location": { "lat": 31.27, "lng": 121.46, "alt": 320 },
  "rangeKm": 12.5,
  "payload": {
    "surfaceTemp": 62.1,
    "pm25": null
  }
}
```
- 说明：`rangeKm` 表示在当前电量/环境下的剩余可续航公里数，`location` 需实时更新。
- 服务端需支持按 `pilot` 过滤（仅推送与当前用户相关的无人机）。

### `GET /ws/mission-events`
- 用途：实时广播任务状态/进度/航线事件，前端用于更新 MissionCommander 中的 `progress`、`milestones` 和任务可选项。
- 请求头：`Sec-WebSocket-Protocol: bearer,<token>`。
- 推送示例：
```json
{
  "type": "mission.progress",
  "missionId": "M-20240601",
  "progress": 58,
  "milestone": "采集完成 70%",
  "assignedUavs": ["UAV-09", "UAV-21"],
  "status": "执行中"
}
```
- 其它事件类型：`mission.interrupted`, `mission.completed`, `mission.assignedUav` 等。

### `GET /ws/monitoring/{taskId}/metrics`
- 用途：`Monitoring` 页面实时指标面板；每条消息更新任务的 `data` 或 `metricTrend`。
- 请求头：`Sec-WebSocket-Protocol: bearer,<token>`。
- 推送示例：
```json
{
  "type": "monitoring.metric",
  "taskId": "T-01",
  "metric": "地表温度",
  "point": { "timestamp": "08:45", "value": 66.5 },
  "snapshot": {
    "label": "地表温度",
    "value": "66.5",
    "unit": "°C"
  }
}
```
- 当达到阈值时可推送 `type: "monitoring.alert"`，携带 `ruleId`, `level`, `message`，供前端提示。

### `GET /ws/monitoring/{taskId}/video`
- 用途：实时图像/视频回传；满足“实时获取无人机状态信息和回传图像”的要求。
- 请求头：`Sec-WebSocket-Protocol: bearer,<token>`，若需二进制帧可协商 `Sec-WebSocket-Extensions: permessage-deflate`。
- 传输方式：
  - 文本帧：`{"type":"frame","contentType":"image/jpeg","payload":"base64"}`。
  - 或二进制帧（H.264/MP4 切片）。
- 服务器需返回 `streamId`，用于 HTTP 接口 `videoUrl` 的占位值。

---

## 错误码与通用约定
- 所有 HTTP 接口使用标准状态码：400（参数错误）、401（未授权）、403（无权限）、404（资源不存在）、409（业务冲突，如无人机已被任务占用）、500（内部错误）。
- 响应体建议统一：
```json
{
  "success": false,
  "errorCode": "FLEET.UAV_BUSY",
  "message": "无人机 UAV-21 已被任务 M-20240601 占用"
}
```
- 时间戳使用 ISO8601（UTC），前端通过 `dayjs` 处理。
- 地理坐标使用 `[纬度, 经度]` 数组或 `{lat,lng}` 对象，两者需保持一致。
- WebSocket 出现鉴权失败时立即发送 `{"type":"error","message":"unauthorized"}` 并断开连接。

---

## 前端对应关系速查
- `Dashboard`：`GET /api/fleet/summary`、`GET /api/missions?status=执行中,排队`、`ws/fleet-status`。
- `FleetCenter`：`GET /api/fleet`、`POST /api/fleet`、`GET /api/fleet/available`、`ws/fleet-status`。
- `MissionCommander`：`GET /api/mission-types`、`GET /api/missions`、`POST /api/missions`、`PATCH`/`interrupt`、`ws/mission-events`。
- `Monitoring`：`GET /api/monitoring/tasks`、`GET /api/monitoring/tasks/{id}`、规则增删改、`ws/monitoring/{taskId}/metrics`、`ws/monitoring/{taskId}/video`。
- `DataAnalytics`：`GET /api/analytics/definitions`、`GET /api/analytics/task-executions`。
- `PersonnelManagement`：无独立接口，直接使用 `GET /api/users`（可额外补充），或沿用账号管理模块。
