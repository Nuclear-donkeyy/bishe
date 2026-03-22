package com.example.uavbackend.auth.dto;

import com.example.uavbackend.auth.DepartmentStatus;

public record DepartmentDto(
    Long id,
    String deptCode,
    String deptName,
    String description,
    DepartmentStatus status,
    int memberCount,
    int leadCount,
    int executorCount) {}
