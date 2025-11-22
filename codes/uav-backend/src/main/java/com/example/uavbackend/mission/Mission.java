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
@Table(name = "missions")
public class Mission extends BaseEntity {
  @Column(name = "mission_code", nullable = false, unique = true, length = 64)
  private String missionCode;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "mission_type", nullable = false, length = 64)
  private String missionType;

  @Column(name = "pilot_name", nullable = false, length = 64)
  private String pilotName;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false, length = 8)
  private String priority;

  @Column(nullable = false)
  private Integer progress = 0;

  @Column(name = "color_hex", length = 7)
  private String colorHex;

  @Column(columnDefinition = "json")
  private String metrics;

  @Column(columnDefinition = "json")
  private String milestones;
}
