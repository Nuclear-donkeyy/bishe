package com.example.uavbackend.configcenter;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("metric_definitions")
public class MetricDefinition extends BaseEntity {
  private String metricCode;
  private String name;
  private String unit;
  private String description;
}
