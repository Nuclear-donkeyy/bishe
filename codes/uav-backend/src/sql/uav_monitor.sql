/*
 Navicat Premium Dump SQL

 Source Server         : root
 Source Server Type    : MySQL
 Source Server Version : 80407 (8.4.7)
 Source Host           : localhost:3307
 Source Schema         : uav_monitor

 Target Server Type    : MySQL
 Target Server Version : 80407 (8.4.7)
 File Encoding         : 65001

 Date: 14/03/2026 12:16:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL,
  `mission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `uav_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `metric_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `metric_value` double NULL DEFAULT NULL,
  `triggered_at` datetime NULL DEFAULT NULL,
  `processed` tinyint(1) NULL DEFAULT 0,
  `processed_at` datetime NULL DEFAULT NULL,
  `linkage_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `linkage_summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notification_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_alert_record_rule`(`rule_id` ASC) USING BTREE,
  CONSTRAINT `fk_alert_record_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_rule
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule`;
CREATE TABLE `alert_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `logic_operator` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `template_enabled` tinyint(1) NULL DEFAULT 0,
  `template_id` bigint NULL DEFAULT NULL,
  `template_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `template_category` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `auto_interrupt` tinyint(1) NULL DEFAULT 0,
  `notify_enabled` tinyint(1) NULL DEFAULT 0,
  `notify_channels` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notify_targets` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `notify_template` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for alert_rule_condition
-- ----------------------------
DROP TABLE IF EXISTS `alert_rule_condition`;
CREATE TABLE `alert_rule_condition`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_id` bigint NOT NULL,
  `metric_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `comparator` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `threshold` double NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_alert_rule_condition_rule`(`rule_id` ASC) USING BTREE,
  CONSTRAINT `fk_alert_rule_condition_rule` FOREIGN KEY (`rule_id`) REFERENCES `alert_rule` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for analytics_definitions
-- ----------------------------
DROP TABLE IF EXISTS `analytics_definitions`;
CREATE TABLE `analytics_definitions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `series_config` json NOT NULL,
  `display_order` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for auth_tokens
-- ----------------------------
DROP TABLE IF EXISTS `auth_tokens`;
CREATE TABLE `auth_tokens`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL,
  `token_hash` char(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expires_at` datetime NOT NULL,
  `revoked_at` datetime NULL DEFAULT NULL,
  `client_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tokens_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_tokens_hash`(`token_hash` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for metric_definitions
-- ----------------------------
DROP TABLE IF EXISTS `metric_definitions`;
CREATE TABLE `metric_definitions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `metric_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `unit` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `metric_code`(`metric_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for metric_sensors
-- ----------------------------
DROP TABLE IF EXISTS `metric_sensors`;
CREATE TABLE `metric_sensors`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `metric_id` bigint UNSIGNED NOT NULL,
  `sensor_type_id` bigint UNSIGNED NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_metric`(`metric_id` ASC) USING BTREE,
  INDEX `idx_sensor`(`sensor_type_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_data_record
-- ----------------------------
DROP TABLE IF EXISTS `mission_data_record`;
CREATE TABLE `mission_data_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mission_id` bigint NULL DEFAULT NULL,
  `mission_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `mission_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pilot_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `uav_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `start_time` datetime NULL DEFAULT NULL,
  `end_time` datetime NULL DEFAULT NULL,
  `data_max` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `data_min` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `data_avg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_events
-- ----------------------------
DROP TABLE IF EXISTS `mission_events`;
CREATE TABLE `mission_events`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_id` bigint UNSIGNED NOT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `payload` json NULL,
  `occurred_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_events_mission`(`mission_id` ASC, `occurred_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_route_points
-- ----------------------------
DROP TABLE IF EXISTS `mission_route_points`;
CREATE TABLE `mission_route_points`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_id` bigint UNSIGNED NOT NULL,
  `seq` int NOT NULL,
  `lat` decimal(9, 6) NOT NULL,
  `lng` decimal(9, 6) NOT NULL,
  `altitude` decimal(8, 2) NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_route_mission_seq`(`mission_id` ASC, `seq` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 150 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_type_metrics
-- ----------------------------
DROP TABLE IF EXISTS `mission_type_metrics`;
CREATE TABLE `mission_type_metrics`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_type_id` bigint UNSIGNED NOT NULL,
  `metric_id` bigint UNSIGNED NOT NULL,
  `display_order` int NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mtm`(`mission_type_id` ASC, `metric_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_types
-- ----------------------------
DROP TABLE IF EXISTS `mission_types`;
CREATE TABLE `mission_types`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `type_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `display_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `recommended_sensors` json NULL,
  `metrics` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `type_code`(`type_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for mission_uav_assignments
-- ----------------------------
DROP TABLE IF EXISTS `mission_uav_assignments`;
CREATE TABLE `mission_uav_assignments`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_id` bigint UNSIGNED NOT NULL,
  `uav_id` bigint UNSIGNED NOT NULL,
  `assigned_at` datetime NOT NULL,
  `released_at` datetime NULL DEFAULT NULL,
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assign_mission`(`mission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for missions
-- ----------------------------
DROP TABLE IF EXISTS `missions`;
CREATE TABLE `missions`  (
  `rule_id` bigint NULL DEFAULT NULL,
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `mission_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mission_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `pilot_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `priority` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `progress` int NOT NULL DEFAULT 0,
  `color_hex` char(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `metrics` json NULL,
  `milestones` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `mission_code`(`mission_code` ASC) USING BTREE,
  INDEX `idx_missions_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 34 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitoring_rules
-- ----------------------------
DROP TABLE IF EXISTS `monitoring_rules`;
CREATE TABLE `monitoring_rules`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_id` bigint UNSIGNED NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `metric` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `threshold` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rules_task`(`task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for monitoring_tasks
-- ----------------------------
DROP TABLE IF EXISTS `monitoring_tasks`;
CREATE TABLE `monitoring_tasks`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `task_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mission_id` bigint UNSIGNED NULL DEFAULT NULL,
  `mission_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mission_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `owner_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `location_desc` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `devices_count` int NULL DEFAULT NULL,
  `video_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `stream_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `extra_data` json NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_code`(`task_code` ASC) USING BTREE,
  INDEX `idx_monitoring_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sensor_types
-- ----------------------------
DROP TABLE IF EXISTS `sensor_types`;
CREATE TABLE `sensor_types`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `sensor_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `sensor_code`(`sensor_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for task_executions
-- ----------------------------
DROP TABLE IF EXISTS `task_executions`;
CREATE TABLE `task_executions`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `execution_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mission_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `mission_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `owner_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `completed_at` datetime NOT NULL,
  `metrics` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `execution_code`(`execution_code` ASC) USING BTREE,
  INDEX `idx_exec_type_time`(`mission_type` ASC, `completed_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for uav_devices
-- ----------------------------
DROP TABLE IF EXISTS `uav_devices`;
CREATE TABLE `uav_devices`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `uav_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `pilot_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uav_code`(`uav_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for uav_telemetry
-- ----------------------------
DROP TABLE IF EXISTS `uav_telemetry`;
CREATE TABLE `uav_telemetry`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `uav_id` bigint UNSIGNED NOT NULL,
  `session_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `reported_at` datetime NOT NULL,
  `battery_percent` int NULL DEFAULT NULL,
  `range_km` decimal(8, 2) NULL DEFAULT NULL,
  `location_lat` decimal(9, 6) NULL DEFAULT NULL,
  `location_lng` decimal(9, 6) NULL DEFAULT NULL,
  `location_alt` decimal(8, 2) NULL DEFAULT NULL,
  `velocity_ms` decimal(8, 2) NULL DEFAULT NULL,
  `payload` json NULL,
  `raw_message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uav_telemetry_uav_time`(`uav_id` ASC, `reported_at` ASC) USING BTREE,
  INDEX `idx_uav_telemetry_session_time`(`session_code` ASC, `reported_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for uav_sensors
-- ----------------------------
DROP TABLE IF EXISTS `uav_sensors`;
CREATE TABLE `uav_sensors`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `uav_id` bigint UNSIGNED NOT NULL,
  `sensor_type_id` bigint UNSIGNED NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_uav`(`uav_id` ASC) USING BTREE,
  INDEX `idx_sensor`(`sensor_type_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` enum('SUPERADMIN','OPERATOR') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OPERATOR',
  `status` enum('ACTIVE','DISABLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` datetime NULL DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
