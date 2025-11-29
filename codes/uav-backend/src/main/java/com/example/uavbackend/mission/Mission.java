package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("missions")
public class Mission extends BaseEntity {
  private String missionCode;
  private String name;
  private String missionType;
  private String pilotName;
  private String status;
  private String priority;
  private Integer progress = 0;
  private String colorHex;
  private String metrics;
  private String milestones;
  private Long ruleId;
}
