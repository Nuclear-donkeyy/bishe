package com.example.uavbackend.fleet;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "uav_sessions")
public class UavSession extends BaseEntity {
  @Column(name = "uav_id", nullable = false)
  private Long uavId;

  @Column(name = "session_code", nullable = false, length = 32)
  private String sessionCode;

  @Column(name = "client_id", length = 128)
  private String clientId;

  @Column(name = "connected_at", nullable = false)
  private Instant connectedAt;

  @Column(name = "disconnected_at")
  private Instant disconnectedAt;

  @Column(name = "connection_ip", length = 45)
  private String connectionIp;

  @Column(length = 255)
  private String reason;
}
