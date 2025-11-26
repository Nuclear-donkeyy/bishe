package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("task_executions")
public class TaskExecution extends BaseEntity {
  private String executionCode;
  private String missionName;
  private String missionType;
  private String location;
  private String ownerName;
  private Instant completedAt;
  private String metrics;
}
