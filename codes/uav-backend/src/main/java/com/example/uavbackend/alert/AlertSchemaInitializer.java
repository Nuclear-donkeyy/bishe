package com.example.uavbackend.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertSchemaInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(org.springframework.boot.ApplicationArguments args) {
    ensureColumn("alert_rule", "template_enabled", "ALTER TABLE alert_rule ADD COLUMN template_enabled tinyint(1) NULL DEFAULT 0");
    ensureColumn("alert_rule", "template_id", "ALTER TABLE alert_rule ADD COLUMN template_id bigint NULL DEFAULT NULL");
    ensureColumn("alert_rule", "template_code", "ALTER TABLE alert_rule ADD COLUMN template_code varchar(100) NULL DEFAULT NULL");
    ensureColumn(
        "alert_rule",
        "template_category",
        "ALTER TABLE alert_rule ADD COLUMN template_category varchar(100) NULL DEFAULT NULL");
    ensureColumn("alert_rule", "auto_interrupt", "ALTER TABLE alert_rule ADD COLUMN auto_interrupt tinyint(1) NULL DEFAULT 0");
    ensureColumn("alert_rule", "notify_enabled", "ALTER TABLE alert_rule ADD COLUMN notify_enabled tinyint(1) NULL DEFAULT 0");
    ensureColumn(
        "alert_rule",
        "notify_channels",
        "ALTER TABLE alert_rule ADD COLUMN notify_channels varchar(100) NULL DEFAULT NULL");
    ensureColumn(
        "alert_rule",
        "notify_targets",
        "ALTER TABLE alert_rule ADD COLUMN notify_targets varchar(500) NULL DEFAULT NULL");
    ensureColumn(
        "alert_rule",
        "notify_template",
        "ALTER TABLE alert_rule ADD COLUMN notify_template varchar(500) NULL DEFAULT NULL");

    ensureColumn(
        "alert_record",
        "linkage_status",
        "ALTER TABLE alert_record ADD COLUMN linkage_status varchar(20) NULL DEFAULT NULL");
    ensureColumn(
        "alert_record",
        "linkage_summary",
        "ALTER TABLE alert_record ADD COLUMN linkage_summary varchar(500) NULL DEFAULT NULL");
    ensureColumn(
        "alert_record",
        "notification_status",
        "ALTER TABLE alert_record ADD COLUMN notification_status varchar(30) NULL DEFAULT NULL");
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
