package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("uav_devices")
public class UavDevice extends BaseEntity {
  private String uavCode;
  private String model;
  private String pilotName;
  private UavStatus status = UavStatus.PENDING_CONNECT;
}
