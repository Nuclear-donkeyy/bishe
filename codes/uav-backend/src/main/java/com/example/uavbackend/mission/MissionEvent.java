package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mission_events")
public class MissionEvent extends BaseEntity {
  private Long missionId;
  private String eventType;
  private String payload;
  private Instant occurredAt;
}
