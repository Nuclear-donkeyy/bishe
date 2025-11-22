package com.example.uavbackend.monitoring;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringTaskRepository extends JpaRepository<MonitoringTask, Long> {
  Optional<MonitoringTask> findByTaskCode(String taskCode);

  List<MonitoringTask> findByStatus(String status);
}
