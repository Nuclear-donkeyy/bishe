package com.example.uavbackend.configcenter;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sensor_types")
public class SensorType extends BaseEntity {
  private String sensorCode;
  private String name;
  private String description;
}
