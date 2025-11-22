package com.example.uavbackend.mission;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRoutePointRepository extends JpaRepository<MissionRoutePoint, Long> {
  List<MissionRoutePoint> findByMissionIdOrderBySeqAsc(Long missionId);
}
