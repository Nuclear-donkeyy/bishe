package com.example.uavbackend.mission;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mission_types")
public class MissionTypeDefinition extends BaseEntity {
  @Column(name = "type_code", nullable = false, unique = true, length = 64)
  private String typeCode;

  @Column(name = "display_name", nullable = false, length = 64)
  private String displayName;

  @Column(length = 255)
  private String description;

  @Column(name = "recommended_sensors", columnDefinition = "json")
  private String recommendedSensors;

  @Column(columnDefinition = "json")
  private String metrics;
}
