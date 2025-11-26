# 后端实时数据与 WebSocket 实施规划（无人机遥测流）

目标：基于 Redis 作为实时状态存储，后端主动定时（100ms）推送 UAV 状态到前端，通过 WebSocket 分发。后续再接入 MQTT broker（此阶段不实现 MQTT，仅做接口与结构预留）。

## 数据模型与 Redis 约定
- Redis Key：`uav:telemetry:{uav_code}`
- Value：JSON 字符串，对应字段示例：
  ```json
  {
    "uavCode": "UAV-001",
    "model": "PX4",
    "pilotName": "张三",
    "sensors": ["THERMAL", "VISIBLE"],
    "batteryPercent": 78,
    "status": "ONLINE",
    "lat": 31.23,
    "lng": 121.47,
    "alt": 120.5,
    "rttMs": 130,
    "linkQuality": "良",
    "timestamp": "2024-06-18T08:23:00Z"
  }
  ```
- 补充说明：
  - `batteryPercent/status/lat/lng/alt/linkQuality/rttMs` 等为实时字段；数据库表已去除这些字段，完全以 Redis 为准。
  - `pilotName/model/sensors` 可从 UAV 静态表查到，Redis payload 可以冗余一份，便于前端直接渲染。

## 服务端组件
1) Redis 依赖与配置
   - Maven：`spring-boot-starter-data-redis`（lettuce），配置 `spring.data.redis.*`。
   - 提供 `RedisTemplate<String, String>` 或 `StringRedisTemplate`。

2) WebSocket 推送
   - 使用 Spring WebSocket + STOMP（或原生 WebSocket），推荐 endpoint：`/ws/uav-telemetry`。
   - 订阅主题：`/topic/uav-telemetry`（广播所有 UAV 更新）；可选支持按 uavCode 订阅 `/topic/uav-telemetry/{uavCode}`。
   - 定时任务（100ms）：扫描 Redis key 前缀 `uav:telemetry:*`，批量读取 value，反序列化为对象列表，推送到 `/topic/uav-telemetry`。
   - 鉴权：复用现有 token 过滤器或在握手阶段校验 Authorization header。

3) 数据写入 Redis（后续接入 MQTT）
   - 预留 Service 方法：`UavTelemetryService.upsertTelemetry(UavTelemetryPayload payload)`，将 payload 序列化写入 Redis。
   - 现阶段可提供一个模拟接口/定时 mock，用于写入 Redis 测试（例如 `/api/fleet/mock-telemetry` 接口写入 demo 数据）。

## 前端对接
1) WebSocket 客户端
   - 使用浏览器 WebSocket 连接 `ws://localhost:8080/ws/uav-telemetry`，带 token（Sec-WebSocket-Protocol: bearer,<token>）。
   - 监听消息，payload 即 Redis JSON，对应渲染：
     - 表格列：uavCode、model、sensors（标签）、pilotName、batteryPercent、status、lat/lng/alt。
     - 未到达的实时字段（电量/状态/经纬度）在 UI 中显示 “--” 或留空。

2) 状态管理
   - 前端维护 `Map<uavCode, telemetry>`，收到消息时更新。
   - 表格合并：静态列表（/fleet 接口返回 uavCode、model、pilotName、sensors） + 实时 Map（battery/status/lat/lng/alt）。

## 实施步骤
1) 后端依赖与配置
   - 添加 `spring-boot-starter-data-redis`。
   - 在 `application.yml` 增加 Redis 配置。

2) WebSocket 基础
   - 配置 `WebSocketConfig`：启用 STOMP endpoint `/ws/uav-telemetry`，允许跨域；广播目的地 `/topic/uav-telemetry`。
   - 在 SecurityConfig 放行握手端点，或在握手拦截器校验 token。

3) 实时推送定时任务
   - 新建 `TelemetryPushTask`：`@Scheduled(fixedDelay = 100)`。
   - 使用 `StringRedisTemplate` 扫描 keys `uav:telemetry:*`，取值后推送到 `/topic/uav-telemetry`。
   - 控制 payload 大小：如有需要可分页或只推最近变更。

4) 写入 Redis 的接口（用于测试/模拟）
   - 可选新增 `POST /api/fleet/mock-telemetry`，接受 payload 写入 Redis，便于联调。
   - 最终 MQTT 接入时，消息到达后调用同样的 `upsertTelemetry` 服务。

5) 前端改造
   - FleetCenter：
     - 表格数据 = 后端静态列表 + 实时 Map。电量/状态/经纬度从实时 Map 取值，未取到则显示 “--”。
     - WebSocket 客户端连接 `/ws/uav-telemetry`，订阅 `/topic/uav-telemetry`。
   - 可选：新增一个 “实时数据” 面板展示当前心跳时间/上次更新时间。

6) 数据库调整
   - 已去除 UAV 表的实时字段；新增 UAV-Sensor 关系表：
     ```sql
     CREATE TABLE uav_sensors (
       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
       uav_id BIGINT UNSIGNED NOT NULL,
       sensor_type_id BIGINT UNSIGNED NOT NULL,
       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       KEY idx_uav (uav_id),
       KEY idx_sensor (sensor_type_id)
     );
     ```
   - Redis 负责实时字段。

## 先期可交付内容（第一迭代）
1) 后端：Redis 配置 + WebSocket 配置 + 定时推送任务 + Mock 写入接口。
2) 前端：WebSocket 客户端接入，表格合并实时数据，新增/编辑表单保持最新字段（uavCode, model, pilotUsername, sensors）。

后续迭代：接入 MQTT 实际数据源，增加报警/状态逻辑。
