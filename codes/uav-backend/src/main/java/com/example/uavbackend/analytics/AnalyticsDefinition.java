package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("analytics_definitions")
public class AnalyticsDefinition extends BaseEntity {
  private String missionType;
  private String title;
  private String description;
  private String seriesConfig;
  private Integer displayOrder;
}
