package com.example.uavbackend.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class DepartmentSchemaInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS departments (
          id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
          dept_code VARCHAR(64) NOT NULL,
          dept_name VARCHAR(128) NOT NULL,
          description VARCHAR(255) NULL DEFAULT NULL,
          status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (id),
          UNIQUE KEY uk_departments_code (dept_code)
        )
        """);

    ensureColumn("users", "department_id", "ALTER TABLE users ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn("users", "department_name", "ALTER TABLE users ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");
    normalizeUserRoleColumn();

    ensureColumn("uav_devices", "department_id", "ALTER TABLE uav_devices ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn(
        "uav_devices",
        "department_name",
        "ALTER TABLE uav_devices ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");
    ensureColumn(
        "uav_devices",
        "owner_username",
        "ALTER TABLE uav_devices ADD COLUMN owner_username VARCHAR(64) NULL DEFAULT NULL");

    ensureColumn("alert_rule", "department_id", "ALTER TABLE alert_rule ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn(
        "alert_rule",
        "department_name",
        "ALTER TABLE alert_rule ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");
    ensureColumn(
        "alert_rule",
        "created_by",
        "ALTER TABLE alert_rule ADD COLUMN created_by VARCHAR(64) NULL DEFAULT NULL");

    ensureColumn("missions", "department_id", "ALTER TABLE missions ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn(
        "missions",
        "department_name",
        "ALTER TABLE missions ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");
    ensureColumn(
        "missions",
        "pilot_username",
        "ALTER TABLE missions ADD COLUMN pilot_username VARCHAR(64) NULL DEFAULT NULL");

    ensureColumn(
        "mission_data_record",
        "department_id",
        "ALTER TABLE mission_data_record ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn(
        "mission_data_record",
        "department_name",
        "ALTER TABLE mission_data_record ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");

    ensureColumn(
        "task_executions",
        "department_id",
        "ALTER TABLE task_executions ADD COLUMN department_id BIGINT NULL DEFAULT NULL");
    ensureColumn(
        "task_executions",
        "department_name",
        "ALTER TABLE task_executions ADD COLUMN department_name VARCHAR(128) NULL DEFAULT NULL");

    log.info("Ensured department RBAC schema");
  }

  private void normalizeUserRoleColumn() {
    String columnType =
        jdbcTemplate.queryForObject(
            """
            SELECT column_type
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'role'
            """,
            String.class);
    if (columnType == null) {
      return;
    }
    if (columnType.startsWith("enum(")) {
      jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL DEFAULT 'EXECUTOR'");
    }
    jdbcTemplate.update("UPDATE users SET role = 'EXECUTOR' WHERE role = 'OPERATOR'");
  }

  private void ensureColumn(String tableName, String columnName, String ddl) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """,
            Integer.class,
            tableName,
            columnName);
    if (count != null && count > 0) {
      return;
    }
    jdbcTemplate.execute(ddl);
    log.info("Added schema column {}.{}", tableName, columnName);
  }
}
