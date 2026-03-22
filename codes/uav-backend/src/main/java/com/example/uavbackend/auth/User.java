package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("users")
public class User extends BaseEntity {
  private String username;
  private String passwordHash;
  private String name;
  private Long departmentId;
  private String departmentName;
  private UserRole role = UserRole.EXECUTOR;
  private UserStatus status = UserStatus.ACTIVE;
  private Instant lastLoginAt;
}
