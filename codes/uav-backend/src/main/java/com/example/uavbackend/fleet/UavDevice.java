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
  private String linkQuality;
  private Integer batteryPercent;
  private BigDecimal rangeKm;
  private Integer rttMs;
  private BigDecimal locationLat;
  private BigDecimal locationLng;
  private BigDecimal locationAlt;
  private Instant lastHeartbeatAt;
  private Long currentMissionId;
  private String connectionEndpoint;
  private String connectionProtocol;
  private String connectionSecret;
  private String telemetryTopics;
  private String mqttUsername;
  private String mqttPassword;
  private String metadata;
}
