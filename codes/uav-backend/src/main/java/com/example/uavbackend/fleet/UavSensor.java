package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("uav_sensors")
public class UavSensor extends BaseEntity {
  private Long uavId;
  private Long sensorTypeId;
}
