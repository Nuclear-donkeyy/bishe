# 后端 Redis + WebSocket 实时遥测说明

本说明基于 Redis 7.4 + Spring Boot 3，当前实现了：
- Redis 存储 UAV 实时遥测（key 前缀 `uav:telemetry:`）。
- WebSocket（STOMP）每 100ms 推送 Redis 中的最新数据到前端。
- Mock 接口 `/api/fleet/mock-telemetry` 可写入测试数据。

## 依赖与配置
- `pom.xml`：添加 `spring-boot-starter-data-redis`；已有 `spring-boot-starter-websocket`。
- `application.yml` Redis 配置示例：
  ```yaml
  spring:
    data:
      redis:
        host: localhost
        port: 6379
        database: 0
        timeout: 5000
  ```

## Redis 约定
- Key：`uav:telemetry:{uav_code}`
- Value：任意合法 JSON 字符串（后端不解析结构，原样推送）。
  - 推荐字段：`uavCode`, `model`, `pilotName`, `sensors`, `batteryPercent`, `status`, `lat`, `lng`, `alt`, `rttMs`, `linkQuality`, `timestamp`。

## WebSocket
- STOMP endpoint：`/ws/uav-telemetry`（允许跨域，SockJS 可选）。
- 主题：
  - `/topic/uav-telemetry`：广播所有 UAV 更新。
  - `/topic/uav-telemetry/{uavCode}`：按 UAV 维度的子主题。
- 安全：`/ws/**` 已在 SecurityConfig 放行；如需 token 校验，可在握手拦截器中校验 Authorization。

## 定时推送
- `TelemetryPushScheduler`：`@Scheduled(fixedDelay = 100)`。
  - 扫描 `uav:telemetry:*`，读取值，逐条推送到 `/topic/uav-telemetry` 与 `/topic/uav-telemetry/{uavCode}`。
  - 空数据不推送。

## 服务与接口
- `TelemetryService`：
  - `upsertTelemetry(uavCode, json)`：写 Redis。
  - `readAllTelemetry()`：批量读取全部键值。
- Mock 写入接口（便于联调）：
  - `POST /api/fleet/mock-telemetry`
  - Body 示例：
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
      "timestamp": "2024-06-18T08:23:00Z"
    }
    ```

## 数据库
- UAV 表已精简（移除链路/位置等实时字段）。
- UAV-Sensor 关系表需建表：
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

## 前端接入提示
- WebSocket 连接：`ws://localhost:8080/ws/uav-telemetry`，订阅 `/topic/uav-telemetry`。
- 将 `/fleet` 静态列表与实时 Map 合并，填充电量/状态/经纬度等实时字段；未收到实时数据时用 “--” 占位。

## 后续可选
- 在握手阶段校验 token。
- MQTT 消息到达后调用 `TelemetryService.upsertTelemetry` 写入 Redis，替换 mock。
