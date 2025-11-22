package com.example.uavbackend.monitoring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringRuleRepository extends JpaRepository<MonitoringRule, Long> {
  List<MonitoringRule> findByTaskId(Long taskId);
}
