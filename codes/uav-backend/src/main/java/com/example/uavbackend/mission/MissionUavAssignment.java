package com.example.uavbackend.mission;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mission_uav_assignments")
public class MissionUavAssignment extends BaseEntity {
  @Column(name = "mission_id", nullable = false)
  private Long missionId;

  @Column(name = "uav_id", nullable = false)
  private Long uavId;

  @Column(name = "assigned_at", nullable = false)
  private Instant assignedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(length = 32)
  private String role;
}
