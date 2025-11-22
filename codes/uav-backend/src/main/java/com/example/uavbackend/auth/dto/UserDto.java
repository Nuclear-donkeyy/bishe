package com.example.uavbackend.auth.dto;

import com.example.uavbackend.auth.UserRole;

public record UserDto(Long id, String username, String name, UserRole role) {}
