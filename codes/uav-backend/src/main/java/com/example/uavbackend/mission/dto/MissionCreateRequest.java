package com.example.uavbackend.mission.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MissionCreateRequest(
    String missionCode,
    @NotBlank String name,
    @NotBlank String missionType,
    @NotBlank String pilotName,
    @NotBlank String status,
    @NotBlank String priority,
    @Min(0) Integer progress,
    List<String> metrics,
    List<String> milestones,
    @NotEmpty List<List<Double>> route,
    List<String> assignedUavs) {}
