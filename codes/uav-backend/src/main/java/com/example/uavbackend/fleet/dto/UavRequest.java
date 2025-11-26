package com.example.uavbackend.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * UAV 注册/接入申请的请求体，对应 /api/fleet POST。
 */
public record UavRequest(
    @NotBlank String uavCode,
    @NotBlank String model,
    @NotBlank String pilotName,
    @NotNull ConnectionInfo connection,
    String metadata) {
  /** 接入所需连接参数（MQTT/自定义协议）。 */
  public record ConnectionInfo(
      @NotBlank String endpoint,
      @NotBlank String protocol,
      String secret,
      String telemetryTopics,
      String mqttUsername,
      String mqttPassword) {}
}
