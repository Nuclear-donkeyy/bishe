package com.example.uavbackend.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UavRequest(
    @NotBlank String uavCode,
    @NotBlank String model,
    @NotBlank String pilotName,
    @NotNull ConnectionInfo connection,
    String metadata) {
  public record ConnectionInfo(
      @NotBlank String endpoint,
      @NotBlank String protocol,
      String secret,
      String telemetryTopics,
      String mqttUsername,
      String mqttPassword) {}
}
