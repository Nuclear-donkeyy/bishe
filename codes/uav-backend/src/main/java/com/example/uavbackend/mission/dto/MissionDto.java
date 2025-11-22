package com.example.uavbackend.mission.dto;

import java.util.List;

public record MissionDto(
    Long id,
    String missionCode,
    String name,
    String missionType,
    String pilotName,
    String status,
    String priority,
    Integer progress,
    String colorHex,
    List<List<Double>> route,
    List<String> milestones,
    List<String> metrics,
    List<String> assignedUavs) {}
