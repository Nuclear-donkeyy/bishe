package com.example.uavbackend.bootstrap;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(20)
@Slf4j
public class DemoBusinessDataInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  @Override
  public void run(ApplicationArguments args) {
    if (hasBusinessData()) {
      return;
    }
    ResourceDatabasePopulator populator =
        new ResourceDatabasePopulator(new ClassPathResource("db/demo/demo_business_seed.sql"));
    populator.execute(dataSource);
    log.info("Seeded demo business data into business tables");
  }

  private boolean hasBusinessData() {
    return count("sensor_types") > 0
        || count("metric_definitions") > 0
        || count("mission_types") > 0
        || count("missions") > 0
        || count("uav_devices") > 0;
  }

  private long count(String tableName) {
    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
    return count == null ? 0L : count;
  }
}
