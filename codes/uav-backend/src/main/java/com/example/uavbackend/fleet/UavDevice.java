package com.example.uavbackend.fleet;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "uav_devices")
public class UavDevice extends BaseEntity {
  @Column(name = "uav_code", nullable = false, unique = true, length = 64)
  private String uavCode;

  @Column(nullable = false, length = 128)
  private String model;

  @Column(name = "pilot_name", nullable = false, length = 64)
  private String pilotName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private UavStatus status = UavStatus.PENDING_CONNECT;

  @Column(name = "link_quality", length = 8)
  private String linkQuality;

  @Column(name = "battery_percent")
  private Integer batteryPercent;

  @Column(name = "range_km", precision = 8, scale = 2)
  private BigDecimal rangeKm;

  @Column(name = "rtt_ms")
  private Integer rttMs;

  @Column(name = "location_lat", precision = 9, scale = 6)
  private BigDecimal locationLat;

  @Column(name = "location_lng", precision = 9, scale = 6)
  private BigDecimal locationLng;

  @Column(name = "location_alt", precision = 8, scale = 2)
  private BigDecimal locationAlt;

  @Column(name = "last_heartbeat_at")
  private Instant lastHeartbeatAt;

  @Column(name = "current_mission_id")
  private Long currentMissionId;

  @Column(name = "connection_endpoint", nullable = false, length = 255)
  private String connectionEndpoint;

  @Column(name = "connection_protocol", nullable = false, length = 32)
  private String connectionProtocol;

  @Column(name = "connection_secret", length = 128)
  private String connectionSecret;

  @Column(name = "telemetry_topics", columnDefinition = "json")
  private String telemetryTopics;

  @Column(name = "mqtt_username", length = 128)
  private String mqttUsername;

  @Column(name = "mqtt_password", length = 255)
  private String mqttPassword;

  @Column(name = "metadata", columnDefinition = "json")
  private String metadata;
}
