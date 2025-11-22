package com.example.uavbackend.analytics;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
  List<TaskExecution> findByMissionTypeAndCompletedAtBetweenOrderByCompletedAtAsc(
      String missionType, Instant from, Instant to);

  List<TaskExecution> findTop200ByMissionTypeOrderByCompletedAtDesc(String missionType);
}
