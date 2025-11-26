package com.example.uavbackend.configcenter;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mission_type_metrics")
public class MissionTypeMetric extends BaseEntity {
  private Long missionTypeId;
  private Long metricId;
  private Integer displayOrder;
}
