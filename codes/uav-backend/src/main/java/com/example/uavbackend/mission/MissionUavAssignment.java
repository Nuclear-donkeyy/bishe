package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mission_uav_assignments")
public class MissionUavAssignment extends BaseEntity {
  private Long missionId;
  private Long uavId;
  private Instant assignedAt;
  private Instant releasedAt;
  private String role;
}
