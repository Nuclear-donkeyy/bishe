package com.example.uavbackend.auth;

public record AccessScope(boolean superAdmin, String username, String displayName) {
  public boolean isOperator() {
    return !superAdmin;
  }
}
