package com.example.uavbackend.mission;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionTypeRepository extends JpaRepository<MissionTypeDefinition, Long> {
  Optional<MissionTypeDefinition> findByTypeCode(String typeCode);
}
