package com.example.uavbackend.configcenter;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("metric_sensors")
public class MetricSensor extends BaseEntity {
  private Long metricId;
  private Long sensorTypeId;
}
