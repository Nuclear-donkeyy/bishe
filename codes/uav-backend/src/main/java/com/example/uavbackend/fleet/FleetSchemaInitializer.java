package com.example.uavbackend.fleet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(5)
@Slf4j
public class FleetSchemaInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS uav_telemetry (
          id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
          uav_id BIGINT UNSIGNED NOT NULL,
          session_code VARCHAR(128) NULL,
          reported_at DATETIME NOT NULL,
          battery_percent INT NULL,
          range_km DECIMAL(8, 2) NULL,
          location_lat DECIMAL(9, 6) NULL,
          location_lng DECIMAL(9, 6) NULL,
          location_alt DECIMAL(8, 2) NULL,
          velocity_ms DECIMAL(8, 2) NULL,
          payload JSON NULL,
          raw_message LONGTEXT NULL,
          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (id),
          KEY idx_uav_telemetry_uav_time (uav_id, reported_at),
          KEY idx_uav_telemetry_session_time (session_code, reported_at)
        )
        """);
    log.info("Ensured schema table uav_telemetry");
  }
}
