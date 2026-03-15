INSERT INTO sensor_types (id, sensor_code, name, description, created_at, updated_at) VALUES
  (1, 'THERMAL_CAM', '热成像载荷', '用于温度场与热点巡检', '2026-03-10 08:00:00', '2026-03-10 08:00:00'),
  (2, 'RGB_CAM', '可见光相机', '用于航线巡查与图像取证', '2026-03-10 08:00:00', '2026-03-10 08:00:00'),
  (3, 'GAS_BOX', '气体传感器', '用于 PM2.5、CO2 等环境指标采集', '2026-03-10 08:00:00', '2026-03-10 08:00:00'),
  (4, 'WEATHER_BOX', '微型气象站', '用于风速与气象扰动感知', '2026-03-10 08:00:00', '2026-03-10 08:00:00'),
  (5, 'CORONA_SENSOR', '电晕检测载荷', '用于电网局放与电晕监测', '2026-03-10 08:00:00', '2026-03-10 08:00:00'),
  (6, 'MULTI_CAM', '多光谱载荷', '用于地表热区与植被异常识别', '2026-03-10 08:00:00', '2026-03-10 08:00:00');

INSERT INTO metric_definitions (id, metric_code, name, unit, description, created_at, updated_at) VALUES
  (1, 'SURFACE_TEMP', '地表温度', 'C', '火情任务的温度主指标', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (2, 'SMOKE_INDEX', '烟雾指数', '%', '结合图像识别输出的烟雾置信度', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (3, 'FIRE_POINT_COUNT', '火点数量', '个', '热源识别出的疑似火点数', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (4, 'PM25', 'PM2.5', 'ug/m3', '空气颗粒物浓度', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (5, 'CO2', '二氧化碳浓度', 'ppm', '空气剖面 CO2 浓度', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (6, 'WIND_SPEED', '风速', 'm/s', '环境风速', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (7, 'CORONA_INTENSITY', '电晕强度', 'dB', '电网巡检的局放电晕指标', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (8, 'LINE_TEMP', '线夹温度', 'C', '导线与绝缘子热异常温度', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (9, 'BATTERY', '飞行电量', '%', '任务执行过程中的剩余电量', '2026-03-10 08:05:00', '2026-03-10 08:05:00'),
  (10, 'VELOCITY_MS', '飞行速度', 'm/s', '无人机飞行速度', '2026-03-10 08:05:00', '2026-03-10 08:05:00');

INSERT INTO metric_sensors (id, metric_id, sensor_type_id, created_at, updated_at) VALUES
  (1, 1, 1, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (2, 2, 2, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (3, 3, 6, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (4, 4, 3, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (5, 5, 3, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (6, 6, 4, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (7, 7, 5, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (8, 8, 1, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (9, 9, 2, '2026-03-10 08:10:00', '2026-03-10 08:10:00'),
  (10, 10, 2, '2026-03-10 08:10:00', '2026-03-10 08:10:00');

INSERT INTO mission_types (id, type_code, display_name, description, recommended_sensors, metrics, created_at, updated_at) VALUES
  (1, 'FOREST_FIRE', '森林火情巡查', '针对山林热源、烟点与防火带的火情巡查任务', JSON_ARRAY('THERMAL_CAM', 'RGB_CAM', 'MULTI_CAM'), JSON_ARRAY('SURFACE_TEMP', 'SMOKE_INDEX', 'FIRE_POINT_COUNT', 'BATTERY', 'VELOCITY_MS'), '2026-03-10 08:15:00', '2026-03-10 08:15:00'),
  (2, 'AIR_QUALITY', '空气质量剖面', '针对园区和边界区域的空气质量立体监测任务', JSON_ARRAY('GAS_BOX', 'WEATHER_BOX', 'RGB_CAM'), JSON_ARRAY('PM25', 'CO2', 'WIND_SPEED', 'BATTERY', 'VELOCITY_MS'), '2026-03-10 08:15:00', '2026-03-10 08:15:00'),
  (3, 'GRID_PATROL', '电网通道巡检', '针对杆塔、电晕和热异常的电网巡检任务', JSON_ARRAY('THERMAL_CAM', 'CORONA_SENSOR', 'RGB_CAM'), JSON_ARRAY('CORONA_INTENSITY', 'LINE_TEMP', 'BATTERY', 'VELOCITY_MS'), '2026-03-10 08:15:00', '2026-03-10 08:15:00');

INSERT INTO mission_type_metrics (id, mission_type_id, metric_id, display_order, created_at, updated_at) VALUES
  (1, 1, 1, 0, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (2, 1, 2, 1, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (3, 1, 3, 2, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (4, 2, 4, 0, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (5, 2, 5, 1, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (6, 2, 6, 2, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (7, 3, 7, 0, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (8, 3, 8, 1, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (9, 3, 9, 2, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (10, 1, 9, 3, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (11, 1, 10, 4, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (12, 2, 9, 3, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (13, 2, 10, 4, '2026-03-10 08:20:00', '2026-03-10 08:20:00'),
  (14, 3, 10, 3, '2026-03-10 08:20:00', '2026-03-10 08:20:00');

INSERT INTO analytics_definitions (id, mission_type, title, description, series_config, display_order, created_at, updated_at) VALUES
  (1, 'FOREST_FIRE', '任务时长与速度', '查看火情巡查任务的执行效率变化', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', '任务时长(分钟)', 'dataKey', 'derived::durationMinutes'), JSON_OBJECT('name', '平均速度(km/h)', 'dataKey', 'derived::avgSpeedKmh'))), 0, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (2, 'FOREST_FIRE', '温度与烟雾趋势', '查看火情指标的均值与峰值', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', '地表温度均值', 'dataKey', 'avg::SURFACE_TEMP'), JSON_OBJECT('name', '地表温度峰值', 'dataKey', 'max::SURFACE_TEMP'), JSON_OBJECT('name', '烟雾指数均值', 'dataKey', 'avg::SMOKE_INDEX'))), 1, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (3, 'FOREST_FIRE', '火点数量与电量消耗', '观察任务负载与能耗变化', JSON_OBJECT('chartType', 'bar', 'series', JSON_ARRAY(JSON_OBJECT('name', '火点数量峰值', 'dataKey', 'max::FIRE_POINT_COUNT'), JSON_OBJECT('name', '电量消耗(%)', 'dataKey', 'derived::batteryConsumption'))), 2, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (4, 'AIR_QUALITY', '任务时长与速度', '查看空气巡检任务的执行效率变化', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', '任务时长(分钟)', 'dataKey', 'derived::durationMinutes'), JSON_OBJECT('name', '平均速度(km/h)', 'dataKey', 'derived::avgSpeedKmh'))), 0, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (5, 'AIR_QUALITY', 'PM2.5 与 CO2 趋势', '观察空气负载指标的变化情况', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', 'PM2.5 均值', 'dataKey', 'avg::PM25'), JSON_OBJECT('name', 'PM2.5 峰值', 'dataKey', 'max::PM25'), JSON_OBJECT('name', 'CO2 均值', 'dataKey', 'avg::CO2'))), 1, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (6, 'AIR_QUALITY', '风速与电量消耗', '观察风场环境与飞行能耗', JSON_OBJECT('chartType', 'bar', 'series', JSON_ARRAY(JSON_OBJECT('name', '风速均值', 'dataKey', 'avg::WIND_SPEED'), JSON_OBJECT('name', '电量消耗(%)', 'dataKey', 'derived::batteryConsumption'))), 2, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (7, 'GRID_PATROL', '任务时长与速度', '查看电网巡检任务的执行效率变化', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', '任务时长(分钟)', 'dataKey', 'derived::durationMinutes'), JSON_OBJECT('name', '平均速度(km/h)', 'dataKey', 'derived::avgSpeedKmh'))), 0, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (8, 'GRID_PATROL', '电晕强度与线温', '观察局放与热异常水平', JSON_OBJECT('chartType', 'line', 'series', JSON_ARRAY(JSON_OBJECT('name', '电晕强度均值', 'dataKey', 'avg::CORONA_INTENSITY'), JSON_OBJECT('name', '线夹温度峰值', 'dataKey', 'max::LINE_TEMP'))), 1, '2026-03-10 08:30:00', '2026-03-10 08:30:00'),
  (9, 'GRID_PATROL', '告警次数与成功率', '观察巡检异常与任务完成情况', JSON_OBJECT('chartType', 'bar', 'series', JSON_ARRAY(JSON_OBJECT('name', '告警次数', 'dataKey', 'derived::alertCount'), JSON_OBJECT('name', '执行成功率', 'dataKey', 'derived::successRate'))), 2, '2026-03-10 08:30:00', '2026-03-10 08:30:00');

INSERT INTO uav_devices (id, uav_code, model, pilot_name, created_at, updated_at) VALUES
  (1, 'DEMO-FIRE-01', 'Matrice 350 RTK', '张三', '2026-03-10 08:40:00', '2026-03-10 08:40:00'),
  (2, 'DEMO-AIR-01', 'Matrice 300 RTK', '李四', '2026-03-10 08:40:00', '2026-03-10 08:40:00'),
  (3, 'DEMO-GRID-01', 'Matrice 30T', '王五', '2026-03-10 08:40:00', '2026-03-10 08:40:00'),
  (4, 'DEMO-RESERVE-01', 'Matrice 30T', '张三', '2026-03-10 08:40:00', '2026-03-10 08:40:00');

INSERT INTO uav_sensors (id, uav_id, sensor_type_id, created_at, updated_at) VALUES
  (1, 1, 1, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (2, 1, 2, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (3, 1, 6, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (4, 2, 3, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (5, 2, 4, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (6, 2, 2, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (7, 3, 1, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (8, 3, 5, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (9, 3, 2, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (10, 4, 2, '2026-03-10 08:45:00', '2026-03-10 08:45:00'),
  (11, 4, 1, '2026-03-10 08:45:00', '2026-03-10 08:45:00');

INSERT INTO alert_rule (id, name, description, logic_operator, template_enabled, template_id, template_code, template_category, auto_interrupt, notify_enabled, notify_channels, notify_targets, notify_template, created_at, updated_at) VALUES
  (1, '火情联动模板', '森林火情任务的基础报警模板', 'AND', 1, NULL, 'TMP_FIRE', '安全联动', 1, 1, 'SMS', '值班长,林区站', '火情模板通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00'),
  (2, '林区火情升级联动', '继承火情模板，用于高优任务的自动联动', 'AND', 0, 1, 'TMP_FIRE', '安全联动', 1, 1, 'SMS', '值班长,应急小组', '林区火情升级通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00'),
  (3, '空气异常模板', '空气质量任务的基础报警模板', 'AND', 1, NULL, 'TMP_AIR', '环境预警', 0, 1, 'SMS', '园区值班室', '空气异常模板通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00'),
  (4, '园区空气质量联动', '继承空气模板，面向园区边界巡检', 'AND', 0, 3, 'TMP_AIR', '环境预警', 0, 1, 'SMS', '园区值班室,环保专员', '空气质量升级通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00'),
  (5, '低电量保护模板', '通用低电量保护模板', 'OR', 1, NULL, 'TMP_BATTERY', '设备保护', 1, 1, 'SMS', '机务值班', '低电量模板通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00'),
  (6, '低电量回收联动', '继承低电量模板，用于任务自动中断', 'OR', 0, 5, 'TMP_BATTERY', '设备保护', 1, 1, 'SMS', '机务值班,任务负责人', '低电量回收通知', '2026-03-10 08:50:00', '2026-03-10 08:50:00');

INSERT INTO alert_rule_condition (id, rule_id, metric_code, comparator, threshold) VALUES
  (1, 1, 'SURFACE_TEMP', 'GTE', 72),
  (2, 1, 'SMOKE_INDEX', 'GTE', 68),
  (3, 2, 'SURFACE_TEMP', 'GTE', 72),
  (4, 2, 'SMOKE_INDEX', 'GTE', 68),
  (5, 3, 'PM25', 'GTE', 80),
  (6, 3, 'CO2', 'GTE', 900),
  (7, 4, 'PM25', 'GTE', 80),
  (8, 4, 'CO2', 'GTE', 900),
  (9, 5, 'battery', 'LTE', 22),
  (10, 6, 'battery', 'LTE', 22);

INSERT INTO missions (id, rule_id, mission_code, name, mission_type, pilot_name, status, priority, progress, color_hex, metrics, milestones, created_at, updated_at) VALUES
  (1, 2, 'M-HIS-FIRE-0310-A', '北麓林区晨检', '森林火情巡查', '张三', 'COMPLETED', 'HIGH', 100, '#ef4444', JSON_ARRAY('SURFACE_TEMP', 'SMOKE_INDEX', 'FIRE_POINT_COUNT'), JSON_ARRAY('起飞', '山脊热区扫描', '返航复核'), '2026-03-10 09:00:00', '2026-03-10 09:18:00'),
  (2, 2, 'M-HIS-FIRE-0311-B', '南坡烟点复核', '森林火情巡查', '张三', 'COMPLETED', 'HIGH', 100, '#f97316', JSON_ARRAY('SURFACE_TEMP', 'SMOKE_INDEX', 'FIRE_POINT_COUNT'), JSON_ARRAY('起飞', '烟点定点复核', '返航'), '2026-03-11 10:00:00', '2026-03-11 10:22:00'),
  (3, 4, 'M-HIS-AIR-0311-A', '园区空气剖面监测', '空气质量剖面', '李四', 'COMPLETED', 'MEDIUM', 100, '#0ea5e9', JSON_ARRAY('PM25', 'CO2', 'WIND_SPEED'), JSON_ARRAY('起飞', '园区边界剖面飞行', '返航'), '2026-03-11 14:00:00', '2026-03-11 14:28:00'),
  (4, 6, 'M-HIS-GRID-0312-A', '北线杆塔红外巡检', '电网通道巡检', '王五', 'COMPLETED', 'MEDIUM', 100, '#22c55e', JSON_ARRAY('CORONA_INTENSITY', 'LINE_TEMP'), JSON_ARRAY('起飞', '杆塔段巡检', '返航复盘'), '2026-03-12 09:30:00', '2026-03-12 09:52:00'),
  (5, 6, 'M-HIS-GRID-0313-B', '东线绝缘子复检', '电网通道巡检', '王五', 'COMPLETED', 'LOW', 100, '#10b981', JSON_ARRAY('CORONA_INTENSITY', 'LINE_TEMP'), JSON_ARRAY('起飞', '绝缘子复检', '返航'), '2026-03-13 15:00:00', '2026-03-13 15:17:00'),
  (6, 2, 'M-LIVE-FIRE-01', '西侧防火带实时巡查', '森林火情巡查', '张三', 'QUEUE', 'HIGH', 0, '#f59e0b', JSON_ARRAY('SURFACE_TEMP', 'SMOKE_INDEX', 'FIRE_POINT_COUNT'), JSON_ARRAY('起飞准备', '防火带巡查', '返航交接'), '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (7, 4, 'M-LIVE-AIR-01', '化工园边界空气扫描', '空气质量剖面', '李四', 'QUEUE', 'MEDIUM', 0, '#38bdf8', JSON_ARRAY('PM25', 'CO2', 'WIND_SPEED'), JSON_ARRAY('起飞准备', '边界扫描', '返航交接'), '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (8, 6, 'M-LIVE-GRID-01', '北侧通道局放复核', '电网通道巡检', '王五', 'PREEMPTED', 'MEDIUM', 0, '#84cc16', JSON_ARRAY('CORONA_INTENSITY', 'LINE_TEMP'), JSON_ARRAY('起飞准备', '通道复核', '返航交接'), '2026-03-15 09:10:00', '2026-03-15 09:10:00');

INSERT INTO mission_route_points (id, mission_id, seq, lat, lng, altitude, created_at, updated_at) VALUES
  (1, 1, 1, 31.232100, 121.470500, 110, '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (2, 1, 2, 31.233200, 121.472000, 115, '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (3, 1, 3, 31.234400, 121.473400, 118, '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (4, 1, 4, 31.233100, 121.474900, 112, '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (5, 2, 1, 31.228700, 121.468200, 105, '2026-03-11 10:00:00', '2026-03-11 10:00:00'),
  (6, 2, 2, 31.229500, 121.469600, 108, '2026-03-11 10:00:00', '2026-03-11 10:00:00'),
  (7, 2, 3, 31.230600, 121.471200, 112, '2026-03-11 10:00:00', '2026-03-11 10:00:00'),
  (8, 2, 4, 31.229800, 121.472300, 109, '2026-03-11 10:00:00', '2026-03-11 10:00:00'),
  (9, 3, 1, 31.225500, 121.478100, 95, '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (10, 3, 2, 31.226600, 121.479600, 98, '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (11, 3, 3, 31.227900, 121.481000, 102, '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (12, 3, 4, 31.226800, 121.482500, 99, '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (13, 4, 1, 31.236000, 121.461000, 120, '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (14, 4, 2, 31.237100, 121.462500, 122, '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (15, 4, 3, 31.238300, 121.463700, 121, '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (16, 4, 4, 31.237500, 121.465300, 123, '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (17, 5, 1, 31.240000, 121.466500, 118, '2026-03-13 15:00:00', '2026-03-13 15:00:00'),
  (18, 5, 2, 31.241200, 121.468100, 120, '2026-03-13 15:00:00', '2026-03-13 15:00:00'),
  (19, 5, 3, 31.242100, 121.469400, 121, '2026-03-13 15:00:00', '2026-03-13 15:00:00'),
  (20, 5, 4, 31.241000, 121.470700, 119, '2026-03-13 15:00:00', '2026-03-13 15:00:00'),
  (21, 6, 1, 31.231000, 121.475000, 100, '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (22, 6, 2, 31.232000, 121.476100, 102, '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (23, 6, 3, 31.233000, 121.477200, 103, '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (24, 6, 4, 31.232300, 121.478100, 101, '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (25, 7, 1, 31.224000, 121.483000, 92, '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (26, 7, 2, 31.225100, 121.484300, 94, '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (27, 7, 3, 31.226100, 121.485300, 96, '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (28, 7, 4, 31.225000, 121.486400, 95, '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (29, 8, 1, 31.238800, 121.459200, 116, '2026-03-15 09:10:00', '2026-03-15 09:10:00'),
  (30, 8, 2, 31.239900, 121.460600, 118, '2026-03-15 09:10:00', '2026-03-15 09:10:00'),
  (31, 8, 3, 31.241100, 121.461800, 118, '2026-03-15 09:10:00', '2026-03-15 09:10:00'),
  (32, 8, 4, 31.240200, 121.463100, 117, '2026-03-15 09:10:00', '2026-03-15 09:10:00');

INSERT INTO mission_uav_assignments (id, mission_id, uav_id, assigned_at, released_at, role, created_at, updated_at) VALUES
  (1, 1, 1, '2026-03-10 09:00:00', '2026-03-10 09:18:00', 'PRIMARY', '2026-03-10 09:00:00', '2026-03-10 09:18:00'),
  (2, 2, 1, '2026-03-11 10:00:00', '2026-03-11 10:22:00', 'PRIMARY', '2026-03-11 10:00:00', '2026-03-11 10:22:00'),
  (3, 3, 2, '2026-03-11 14:00:00', '2026-03-11 14:28:00', 'PRIMARY', '2026-03-11 14:00:00', '2026-03-11 14:28:00'),
  (4, 4, 3, '2026-03-12 09:30:00', '2026-03-12 09:52:00', 'PRIMARY', '2026-03-12 09:30:00', '2026-03-12 09:52:00'),
  (5, 5, 3, '2026-03-13 15:00:00', '2026-03-13 15:17:00', 'PRIMARY', '2026-03-13 15:00:00', '2026-03-13 15:17:00'),
  (6, 6, 1, '2026-03-15 09:00:00', NULL, 'PRIMARY', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (7, 7, 2, '2026-03-15 09:05:00', NULL, 'PRIMARY', '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (8, 8, 3, '2026-03-15 09:10:00', NULL, 'PRIMARY', '2026-03-15 09:10:00', '2026-03-15 09:10:00');

INSERT INTO monitoring_tasks (id, task_code, mission_id, mission_name, mission_type, owner_name, status, location_desc, devices_count, video_url, stream_id, extra_data, created_at, updated_at) VALUES
  (1, 'MT-FIRE-0310-A', 1, '北麓林区晨检', '森林火情巡查', '张三', 'COMPLETED', '北麓林区防火带', 1, NULL, 'stream-fire-0310', JSON_OBJECT('region', '北麓', 'focus', '晨检热区'), '2026-03-10 09:00:00', '2026-03-10 09:18:00'),
  (2, 'MT-AIR-0311-A', 3, '园区空气剖面监测', '空气质量剖面', '李四', 'COMPLETED', '化工园边界', 1, NULL, 'stream-air-0311', JSON_OBJECT('region', '化工园边界', 'focus', '颗粒物剖面'), '2026-03-11 14:00:00', '2026-03-11 14:28:00'),
  (3, 'MT-GRID-0312-A', 4, '北线杆塔红外巡检', '电网通道巡检', '王五', 'COMPLETED', '北线 110kV 杆塔', 1, NULL, 'stream-grid-0312', JSON_OBJECT('region', '北线', 'focus', '局放与温升'), '2026-03-12 09:30:00', '2026-03-12 09:52:00'),
  (4, 'MT-LIVE-FIRE-01', 6, '西侧防火带实时巡查', '森林火情巡查', '张三', 'PENDING', '西侧防火带', 1, NULL, 'stream-fire-live', JSON_OBJECT('region', '西侧防火带', 'focus', '实时巡查'), '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (5, 'MT-LIVE-AIR-01', 7, '化工园边界空气扫描', '空气质量剖面', '李四', 'PENDING', '化工园边界', 1, NULL, 'stream-air-live', JSON_OBJECT('region', '化工园边界', 'focus', '实时扫描'), '2026-03-15 09:05:00', '2026-03-15 09:05:00');

INSERT INTO monitoring_rules (id, task_id, name, metric, threshold, level, created_at, updated_at) VALUES
  (1, 1, '热源峰值预警', 'SURFACE_TEMP', '>=72', 'HIGH', '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (2, 2, '颗粒物超限预警', 'PM25', '>=80', 'MEDIUM', '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (3, 3, '电晕异常预警', 'CORONA_INTENSITY', '>=38', 'MEDIUM', '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (4, 4, '烟雾指数预警', 'SMOKE_INDEX', '>=68', 'HIGH', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (5, 5, 'CO2 浓度预警', 'CO2', '>=900', 'MEDIUM', '2026-03-15 09:05:00', '2026-03-15 09:05:00');

INSERT INTO mission_events (id, mission_id, event_type, payload, occurred_at, created_at, updated_at) VALUES
  (1, 1, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'HIGH'), '2026-03-10 09:00:00', '2026-03-10 09:00:00', '2026-03-10 09:00:00'),
  (2, 1, 'MISSION_DISPATCHED', JSON_OBJECT('uavCode', 'DEMO-FIRE-01'), '2026-03-10 09:02:00', '2026-03-10 09:02:00', '2026-03-10 09:02:00'),
  (3, 1, 'MISSION_COMPLETED', JSON_OBJECT('uavCode', 'DEMO-FIRE-01'), '2026-03-10 09:18:00', '2026-03-10 09:18:00', '2026-03-10 09:18:00'),
  (4, 2, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'HIGH'), '2026-03-11 10:00:00', '2026-03-11 10:00:00', '2026-03-11 10:00:00'),
  (5, 2, 'MISSION_DISPATCHED', JSON_OBJECT('uavCode', 'DEMO-FIRE-01'), '2026-03-11 10:03:00', '2026-03-11 10:03:00', '2026-03-11 10:03:00'),
  (6, 2, 'MISSION_COMPLETED', JSON_OBJECT('uavCode', 'DEMO-FIRE-01'), '2026-03-11 10:22:00', '2026-03-11 10:22:00', '2026-03-11 10:22:00'),
  (7, 3, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'MEDIUM'), '2026-03-11 14:00:00', '2026-03-11 14:00:00', '2026-03-11 14:00:00'),
  (8, 3, 'MISSION_DISPATCHED', JSON_OBJECT('uavCode', 'DEMO-AIR-01'), '2026-03-11 14:04:00', '2026-03-11 14:04:00', '2026-03-11 14:04:00'),
  (9, 3, 'MISSION_COMPLETED', JSON_OBJECT('uavCode', 'DEMO-AIR-01'), '2026-03-11 14:28:00', '2026-03-11 14:28:00', '2026-03-11 14:28:00'),
  (10, 4, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'MEDIUM'), '2026-03-12 09:30:00', '2026-03-12 09:30:00', '2026-03-12 09:30:00'),
  (11, 4, 'MISSION_DISPATCHED', JSON_OBJECT('uavCode', 'DEMO-GRID-01'), '2026-03-12 09:33:00', '2026-03-12 09:33:00', '2026-03-12 09:33:00'),
  (12, 4, 'MISSION_COMPLETED', JSON_OBJECT('uavCode', 'DEMO-GRID-01'), '2026-03-12 09:52:00', '2026-03-12 09:52:00', '2026-03-12 09:52:00'),
  (13, 5, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'LOW'), '2026-03-13 15:00:00', '2026-03-13 15:00:00', '2026-03-13 15:00:00'),
  (14, 5, 'MISSION_DISPATCHED', JSON_OBJECT('uavCode', 'DEMO-GRID-01'), '2026-03-13 15:02:00', '2026-03-13 15:02:00', '2026-03-13 15:02:00'),
  (15, 5, 'MISSION_COMPLETED', JSON_OBJECT('uavCode', 'DEMO-GRID-01'), '2026-03-13 15:17:00', '2026-03-13 15:17:00', '2026-03-13 15:17:00'),
  (16, 6, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'HIGH', 'source', 'demo_seed'), '2026-03-15 09:00:00', '2026-03-15 09:00:00', '2026-03-15 09:00:00'),
  (17, 7, 'MISSION_ENQUEUED', JSON_OBJECT('priority', 'MEDIUM', 'source', 'demo_seed'), '2026-03-15 09:05:00', '2026-03-15 09:05:00', '2026-03-15 09:05:00'),
  (18, 8, 'MISSION_PREEMPTED', JSON_OBJECT('priority', 'MEDIUM', 'source', 'demo_seed'), '2026-03-15 09:10:00', '2026-03-15 09:10:00', '2026-03-15 09:10:00');

INSERT INTO mission_data_record (id, mission_id, mission_code, mission_type, pilot_name, uav_code, operator_name, start_time, end_time, data_max, data_min, data_avg) VALUES
  (1, 1, 'M-HIS-FIRE-0310-A', '森林火情巡查', '张三', 'DEMO-FIRE-01', '张三', '2026-03-10 09:02:00', '2026-03-10 09:18:00', JSON_OBJECT('SURFACE_TEMP', 76.2, 'SMOKE_INDEX', 69.4, 'FIRE_POINT_COUNT', 3, 'battery', 93.0, 'velocityMs', 28.6), JSON_OBJECT('SURFACE_TEMP', 52.1, 'SMOKE_INDEX', 28.7, 'FIRE_POINT_COUNT', 0, 'battery', 71.5, 'velocityMs', 20.4), JSON_OBJECT('SURFACE_TEMP', 63.8, 'SMOKE_INDEX', 45.9, 'FIRE_POINT_COUNT', 1.2, 'battery', 82.2, 'velocityMs', 24.8)),
  (2, 2, 'M-HIS-FIRE-0311-B', '森林火情巡查', '张三', 'DEMO-FIRE-01', '张三', '2026-03-11 10:03:00', '2026-03-11 10:22:00', JSON_OBJECT('SURFACE_TEMP', 74.6, 'SMOKE_INDEX', 67.3, 'FIRE_POINT_COUNT', 2, 'battery', 88.0, 'velocityMs', 27.9), JSON_OBJECT('SURFACE_TEMP', 49.8, 'SMOKE_INDEX', 24.6, 'FIRE_POINT_COUNT', 0, 'battery', 62.8, 'velocityMs', 19.7), JSON_OBJECT('SURFACE_TEMP', 61.9, 'SMOKE_INDEX', 42.4, 'FIRE_POINT_COUNT', 0.9, 'battery', 74.4, 'velocityMs', 23.9)),
  (3, 3, 'M-HIS-AIR-0311-A', '空气质量剖面', '李四', 'DEMO-AIR-01', '李四', '2026-03-11 14:04:00', '2026-03-11 14:28:00', JSON_OBJECT('PM25', 86.4, 'CO2', 921.0, 'WIND_SPEED', 8.8, 'battery', 91.0, 'velocityMs', 26.4), JSON_OBJECT('PM25', 35.2, 'CO2', 512.0, 'WIND_SPEED', 3.4, 'battery', 59.5, 'velocityMs', 18.3), JSON_OBJECT('PM25', 58.7, 'CO2', 708.0, 'WIND_SPEED', 5.9, 'battery', 74.1, 'velocityMs', 22.1)),
  (4, 4, 'M-HIS-GRID-0312-A', '电网通道巡检', '王五', 'DEMO-GRID-01', '王五', '2026-03-12 09:33:00', '2026-03-12 09:52:00', JSON_OBJECT('CORONA_INTENSITY', 41.3, 'LINE_TEMP', 65.2, 'battery', 89.0, 'velocityMs', 25.8), JSON_OBJECT('CORONA_INTENSITY', 14.6, 'LINE_TEMP', 38.9, 'battery', 66.8, 'velocityMs', 17.5), JSON_OBJECT('CORONA_INTENSITY', 24.5, 'LINE_TEMP', 48.1, 'battery', 77.6, 'velocityMs', 21.4)),
  (5, 5, 'M-HIS-GRID-0313-B', '电网通道巡检', '王五', 'DEMO-GRID-01', '王五', '2026-03-13 15:02:00', '2026-03-13 15:17:00', JSON_OBJECT('CORONA_INTENSITY', 33.8, 'LINE_TEMP', 58.7, 'battery', 84.0, 'velocityMs', 24.1), JSON_OBJECT('CORONA_INTENSITY', 12.4, 'LINE_TEMP', 36.8, 'battery', 69.9, 'velocityMs', 18.2), JSON_OBJECT('CORONA_INTENSITY', 21.9, 'LINE_TEMP', 45.6, 'battery', 76.5, 'velocityMs', 20.5));

INSERT INTO task_executions (id, execution_code, mission_name, mission_type, location, owner_name, completed_at, metrics, created_at, updated_at) VALUES
  (1, 'M-HIS-FIRE-0310-A', '北麓林区晨检', '森林火情巡查', '北麓林区防火带', '张三', '2026-03-10 09:18:00', JSON_OBJECT('derived::durationMinutes', 16.0, 'derived::avgSpeedKmh', 89.28, 'derived::batteryConsumption', 21.5, 'derived::alertCount', 1, 'derived::successRate', 100, 'avg::SURFACE_TEMP', 63.8, 'max::SURFACE_TEMP', 76.2, 'min::SURFACE_TEMP', 52.1, 'avg::SMOKE_INDEX', 45.9, 'max::SMOKE_INDEX', 69.4, 'avg::FIRE_POINT_COUNT', 1.2, 'max::FIRE_POINT_COUNT', 3),
   '2026-03-10 09:18:00', '2026-03-10 09:18:00'),
  (2, 'M-HIS-FIRE-0311-B', '南坡烟点复核', '森林火情巡查', '南坡防火点', '张三', '2026-03-11 10:22:00', JSON_OBJECT('derived::durationMinutes', 19.0, 'derived::avgSpeedKmh', 86.04, 'derived::batteryConsumption', 25.2, 'derived::alertCount', 1, 'derived::successRate', 100, 'avg::SURFACE_TEMP', 61.9, 'max::SURFACE_TEMP', 74.6, 'min::SURFACE_TEMP', 49.8, 'avg::SMOKE_INDEX', 42.4, 'max::SMOKE_INDEX', 67.3, 'avg::FIRE_POINT_COUNT', 0.9, 'max::FIRE_POINT_COUNT', 2),
   '2026-03-11 10:22:00', '2026-03-11 10:22:00'),
  (3, 'M-HIS-AIR-0311-A', '园区空气剖面监测', '空气质量剖面', '化工园边界', '李四', '2026-03-11 14:28:00', JSON_OBJECT('derived::durationMinutes', 24.0, 'derived::avgSpeedKmh', 79.56, 'derived::batteryConsumption', 31.5, 'derived::alertCount', 1, 'derived::successRate', 100, 'avg::PM25', 58.7, 'max::PM25', 86.4, 'min::PM25', 35.2, 'avg::CO2', 708.0, 'max::CO2', 921.0, 'avg::WIND_SPEED', 5.9, 'max::WIND_SPEED', 8.8),
   '2026-03-11 14:28:00', '2026-03-11 14:28:00'),
  (4, 'M-HIS-GRID-0312-A', '北线杆塔红外巡检', '电网通道巡检', '北线 110kV 杆塔', '王五', '2026-03-12 09:52:00', JSON_OBJECT('derived::durationMinutes', 19.0, 'derived::avgSpeedKmh', 77.04, 'derived::batteryConsumption', 22.2, 'derived::alertCount', 1, 'derived::successRate', 100, 'avg::CORONA_INTENSITY', 24.5, 'max::CORONA_INTENSITY', 41.3, 'min::CORONA_INTENSITY', 14.6, 'avg::LINE_TEMP', 48.1, 'max::LINE_TEMP', 65.2),
   '2026-03-12 09:52:00', '2026-03-12 09:52:00'),
  (5, 'M-HIS-GRID-0313-B', '东线绝缘子复检', '电网通道巡检', '东线杆塔区', '王五', '2026-03-13 15:17:00', JSON_OBJECT('derived::durationMinutes', 15.0, 'derived::avgSpeedKmh', 73.8, 'derived::batteryConsumption', 14.1, 'derived::alertCount', 0, 'derived::successRate', 100, 'avg::CORONA_INTENSITY', 21.9, 'max::CORONA_INTENSITY', 33.8, 'min::CORONA_INTENSITY', 12.4, 'avg::LINE_TEMP', 45.6, 'max::LINE_TEMP', 58.7),
   '2026-03-13 15:17:00', '2026-03-13 15:17:00');

INSERT INTO alert_record (id, rule_id, mission_code, uav_code, metric_code, metric_value, triggered_at, processed, processed_at, linkage_status, linkage_summary, notification_status) VALUES
  (1, 2, 'M-HIS-FIRE-0310-A', 'DEMO-FIRE-01', 'SURFACE_TEMP', 76.2, '2026-03-10 09:09:00', 1, '2026-03-10 09:09:10', 'SUCCESS', '已记录火情热点并通知值班长', 'PLACEHOLDER'),
  (2, 2, 'M-HIS-FIRE-0311-B', 'DEMO-FIRE-01', 'SMOKE_INDEX', 67.3, '2026-03-11 10:14:00', 1, '2026-03-11 10:14:10', 'SUCCESS', '已记录烟点复核结果并通知应急小组', 'PLACEHOLDER'),
  (3, 4, 'M-HIS-AIR-0311-A', 'DEMO-AIR-01', 'PM25', 86.4, '2026-03-11 14:16:00', 1, '2026-03-11 14:16:10', 'SUCCESS', '已通知环保专员关注空气异常', 'PLACEHOLDER'),
  (4, 6, 'M-HIS-GRID-0312-A', 'DEMO-GRID-01', 'battery', 21.0, '2026-03-12 09:50:00', 1, '2026-03-12 09:50:10', 'SUCCESS', '低电量联动已记录，任务按计划收尾', 'PLACEHOLDER');

INSERT INTO uav_telemetry (id, uav_id, session_code, reported_at, battery_percent, range_km, location_lat, location_lng, location_alt, velocity_ms, payload, raw_message, created_at, updated_at) VALUES
  (1, 1, 'M-HIS-FIRE-0310-A', '2026-03-10 09:05:00', 91, 0.42, 31.232100, 121.470500, 108.0, 24.80, JSON_OBJECT('SURFACE_TEMP', 59.2, 'SMOKE_INDEX', 34.5, 'FIRE_POINT_COUNT', 0), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-FIRE-0310-A'), '2026-03-10 09:05:00', '2026-03-10 09:05:00'),
  (2, 1, 'M-HIS-FIRE-0310-A', '2026-03-10 09:09:00', 85, 0.88, 31.233200, 121.472000, 112.0, 26.10, JSON_OBJECT('SURFACE_TEMP', 71.8, 'SMOKE_INDEX', 61.0, 'FIRE_POINT_COUNT', 2), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-FIRE-0310-A'), '2026-03-10 09:09:00', '2026-03-10 09:09:00'),
  (3, 1, 'M-HIS-FIRE-0310-A', '2026-03-10 09:14:00', 78, 1.31, 31.234400, 121.473400, 115.0, 23.60, JSON_OBJECT('SURFACE_TEMP', 76.2, 'SMOKE_INDEX', 69.4, 'FIRE_POINT_COUNT', 3), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-FIRE-0310-A'), '2026-03-10 09:14:00', '2026-03-10 09:14:00'),
  (4, 1, 'M-HIS-FIRE-0310-A', '2026-03-10 09:18:00', 72, 0.23, 31.233100, 121.474900, 96.0, 0.00, JSON_OBJECT('SURFACE_TEMP', 64.3, 'SMOKE_INDEX', 48.8, 'FIRE_POINT_COUNT', 1), JSON_OBJECT('status', 'RETURNING', 'missionId', 'M-HIS-FIRE-0310-A'), '2026-03-10 09:18:00', '2026-03-10 09:18:00'),
  (5, 2, 'M-HIS-AIR-0311-A', '2026-03-11 14:08:00', 88, 0.51, 31.225500, 121.478100, 92.0, 21.60, JSON_OBJECT('PM25', 44.2, 'CO2', 612.0, 'WIND_SPEED', 4.8), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-AIR-0311-A'), '2026-03-11 14:08:00', '2026-03-11 14:08:00'),
  (6, 2, 'M-HIS-AIR-0311-A', '2026-03-11 14:16:00', 77, 1.12, 31.227900, 121.481000, 98.0, 22.90, JSON_OBJECT('PM25', 86.4, 'CO2', 921.0, 'WIND_SPEED', 8.8), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-AIR-0311-A'), '2026-03-11 14:16:00', '2026-03-11 14:16:00'),
  (7, 2, 'M-HIS-AIR-0311-A', '2026-03-11 14:24:00', 63, 0.66, 31.226800, 121.482500, 95.0, 18.40, JSON_OBJECT('PM25', 58.6, 'CO2', 704.0, 'WIND_SPEED', 5.6), JSON_OBJECT('status', 'RETURNING', 'missionId', 'M-HIS-AIR-0311-A'), '2026-03-11 14:24:00', '2026-03-11 14:24:00'),
  (8, 3, 'M-HIS-GRID-0312-A', '2026-03-12 09:36:00', 86, 0.48, 31.236000, 121.461000, 116.0, 20.10, JSON_OBJECT('CORONA_INTENSITY', 18.4, 'LINE_TEMP', 43.2), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-GRID-0312-A'), '2026-03-12 09:36:00', '2026-03-12 09:36:00'),
  (9, 3, 'M-HIS-GRID-0312-A', '2026-03-12 09:44:00', 76, 1.06, 31.238300, 121.463700, 120.0, 22.70, JSON_OBJECT('CORONA_INTENSITY', 41.3, 'LINE_TEMP', 65.2), JSON_OBJECT('status', 'EXECUTING', 'missionId', 'M-HIS-GRID-0312-A'), '2026-03-12 09:44:00', '2026-03-12 09:44:00'),
  (10, 3, 'M-HIS-GRID-0312-A', '2026-03-12 09:52:00', 67, 0.17, 31.237500, 121.465300, 88.0, 0.00, JSON_OBJECT('CORONA_INTENSITY', 23.1, 'LINE_TEMP', 47.6), JSON_OBJECT('status', 'RETURNING', 'missionId', 'M-HIS-GRID-0312-A'), '2026-03-12 09:52:00', '2026-03-12 09:52:00');
