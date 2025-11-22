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
@Table(name = "mission_events")
public class MissionEvent extends BaseEntity {
  @Column(name = "mission_id", nullable = false)
  private Long missionId;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(columnDefinition = "json")
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;
}
