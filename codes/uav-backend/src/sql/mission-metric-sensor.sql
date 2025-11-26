CREATE TABLE sensor_types (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    sensor_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );

  CREATE TABLE metric_definitions (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    unit VARCHAR(32),
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  );

  CREATE TABLE metric_sensors (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    metric_id BIGINT UNSIGNED NOT NULL,
    sensor_type_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_metric (metric_id),
    KEY idx_sensor (sensor_type_id)
  );

  CREATE TABLE mission_type_metrics (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    mission_type_id BIGINT UNSIGNED NOT NULL,
    metric_id BIGINT UNSIGNED NOT NULL,
    display_order INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_mtm (mission_type_id, metric_id)
  );

  -- 初始传感器
  INSERT INTO sensor_types (sensor_code, name, description) VALUES
   ('THERMAL', '热成像', '热成像/红外'),
   ('VISIBLE', '可见光', '普通可见光相机'),
   ('MET', '气象', '气象探头/风速风向'),
   ('PM', '颗粒物', 'PM2.5/PM10探头');

  -- 初始指标
  INSERT INTO metric_definitions (metric_code, name, unit, description) VALUES
   ('SURFACE_TEMP', '地表温度', '°C', '热成像最高/平均温度'),
   ('SMOKE_PROB', '烟雾识别概率', null, 'AI烟雾概率'),
   ('NDVI', 'NDVI 植被指数', null, '植被健康'),
   ('PM25', 'PM2.5', 'µg/m³', '颗粒物浓度');

  -- 指标-传感器关联
  INSERT INTO metric_sensors (metric_id, sensor_type_id) VALUES
    ((SELECT id FROM metric_definitions WHERE metric_code='SURFACE_TEMP'), (SELECT id FROM sensor_types WHERE
  sensor_code='THERMAL')),
    ((SELECT id FROM metric_definitions WHERE metric_code='SMOKE_PROB'), (SELECT id FROM sensor_types WHERE
  sensor_code='VISIBLE')),
    ((SELECT id FROM metric_definitions WHERE metric_code='NDVI'), (SELECT id FROM sensor_types WHERE
  sensor_code='VISIBLE')),
    ((SELECT id FROM metric_definitions WHERE metric_code='PM25'), (SELECT id FROM sensor_types WHERE
  sensor_code='PM'));

  -- 任务类型-指标关联（假定已有 mission_types 数据，按 type_code 关联）
  -- 示例：给 “森林火情巡查” 关联温度/烟雾
  INSERT INTO mission_type_metrics (mission_type_id, metric_id, display_order)
  SELECT mt.id, md.id, 0
  FROM mission_types mt
  JOIN metric_definitions md ON md.metric_code IN ('SURFACE_TEMP','SMOKE_PROB')
  WHERE mt.type_code = 'FOREST_FIRE' -- 请替换为实际 type_code
  ;