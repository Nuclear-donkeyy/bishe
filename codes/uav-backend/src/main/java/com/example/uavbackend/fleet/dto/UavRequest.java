package com.example.uavbackend.fleet.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * UAV 注册/接入申请的请求体，对应 /api/fleet POST。
 */
public record UavRequest(
    @NotBlank String uavCode,
    @NotBlank String model,
    @NotBlank String pilotUsername,
    List<String> sensors,
    String metadata) {}
