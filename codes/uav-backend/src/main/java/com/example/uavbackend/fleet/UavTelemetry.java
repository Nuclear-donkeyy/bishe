package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("uav_telemetry")
public class UavTelemetry extends BaseEntity {
  private Long uavId;
  private String sessionCode;
  private Instant reportedAt;
  private Integer batteryPercent;
  private BigDecimal rangeKm;
  private BigDecimal locationLat;
  private BigDecimal locationLng;
  private BigDecimal locationAlt;
  private BigDecimal velocityMs;
  private String payload;
  private String rawMessage;

  @TableField(exist = false)
  private Integer rttMs;
}
