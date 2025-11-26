package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mission_types")
public class MissionTypeDefinition extends BaseEntity {
  private String typeCode;
  private String displayName;
  private String description;
  private String recommendedSensors;
  private String metrics;
}
