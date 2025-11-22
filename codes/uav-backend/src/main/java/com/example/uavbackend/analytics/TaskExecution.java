package com.example.uavbackend.analytics;

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
@Table(name = "task_executions")
public class TaskExecution extends BaseEntity {
  @Column(name = "execution_code", nullable = false, unique = true, length = 64)
  private String executionCode;

  @Column(name = "mission_name", nullable = false, length = 128)
  private String missionName;

  @Column(name = "mission_type", nullable = false, length = 64)
  private String missionType;

  @Column(length = 128)
  private String location;

  @Column(name = "owner_name", length = 64)
  private String ownerName;

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt;

  @Column(columnDefinition = "json")
  private String metrics;
}
