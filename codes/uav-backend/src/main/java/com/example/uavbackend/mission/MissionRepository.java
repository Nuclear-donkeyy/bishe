package com.example.uavbackend.mission;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {
  Optional<Mission> findByMissionCode(String missionCode);

  List<Mission> findByStatusIn(List<String> status);
}
