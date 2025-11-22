package com.example.uavbackend.fleet;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "uav_telemetry")
public class UavTelemetry extends BaseEntity {
  @Column(name = "uav_id", nullable = false)
  private Long uavId;

  @Column(name = "session_code", length = 32)
  private String sessionCode;

  @Column(name = "reported_at", nullable = false)
  private Instant reportedAt;

  @Column(name = "battery_percent")
  private Integer batteryPercent;

  @Column(name = "range_km", precision = 8, scale = 2)
  private BigDecimal rangeKm;

  @Column(name = "location_lat", precision = 9, scale = 6)
  private BigDecimal locationLat;

  @Column(name = "location_lng", precision = 9, scale = 6)
  private BigDecimal locationLng;

  @Column(name = "location_alt", precision = 8, scale = 2)
  private BigDecimal locationAlt;

  @Column(name = "velocity_ms", precision = 8, scale = 2)
  private BigDecimal velocityMs;

  @Column(name = "payload", columnDefinition = "json")
  private String payload;

  @Column(name = "raw_message", columnDefinition = "json")
  private String rawMessage;

  @jakarta.persistence.Transient private Integer rttMs;

  public Integer getRttMs() {
    return rttMs;
  }

  public void setRttMs(Integer rttMs) {
    this.rttMs = rttMs;
  }
}
