package com.example.uavbackend.analytics;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsDefinitionRepository extends JpaRepository<AnalyticsDefinition, Long> {
  List<AnalyticsDefinition> findByMissionTypeOrderByDisplayOrderAsc(String missionType);
}
