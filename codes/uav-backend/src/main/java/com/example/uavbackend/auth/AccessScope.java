package com.example.uavbackend.auth;

public record AccessScope(
    boolean superAdmin,
    String username,
    String displayName,
    UserRole role,
    Long departmentId,
    String departmentName) {
  public boolean isDeptLead() {
    return role == UserRole.DEPT_LEAD;
  }

  public boolean isExecutor() {
    return role == UserRole.EXECUTOR;
  }

  public boolean inDepartment(Long targetDepartmentId) {
    return targetDepartmentId != null && targetDepartmentId.equals(departmentId);
  }
}
