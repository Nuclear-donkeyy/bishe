# UAV Backend (Spring Boot)

## 技术栈
- Spring Boot 3.2 (Web, Security, Data JPA, Validation, WebSocket)
- Spring Integration MQTT（消费无人机心跳）
- Flyway + MySQL 8

## 快速开始
1. 准备 MySQL：
   ```sql
   CREATE DATABASE uav_monitor CHARACTER SET utf8mb4;
   ```
2. 修改 `src/main/resources/application.yml` 中的数据源、MQTT Broker 账号。
3. 初始化默认账号：手动插入 `users` 表（密码使用 BCrypt，例如 `BCryptPasswordEncoder` 生成）。
4. 启动：
   ```bash
   ./mvnw spring-boot:run
   ```
5. 调试 MQTT：启动本地 Mosquitto (`docker run -p 1883:1883 eclipse-mosquitto`)，并向 `uav/<UAV-ID>/telemetry` 发送 JSON：
   ```json
   {
     "type": "uav.telemetry",
     "uavId": "UAV-21",
     "battery": 80,
     "rangeKm": 15.2,
     "location": {"lat": 31.27, "lng": 121.46, "alt": 320}
   }
   ```

## 主要模块
- `auth`: 登录/登出、Token 校验。
- `fleet`: 无人机接入、列表、可用无人机查询、MQTT 心跳入库。
- `mission`: 任务类型、任务创建/查询/中断、航线存储与后端校验入口。
- `monitoring`: 实时监测任务、规则增删，留有 WebSocket 拓展点。
- `analytics`: AI 图表配置、任务执行历史查询。

所有 REST 接口均以 `/api` 为前缀，鉴权通过 `Authorization: Bearer <token>`，接口细节参考 `docs/API接口文档.md`。
