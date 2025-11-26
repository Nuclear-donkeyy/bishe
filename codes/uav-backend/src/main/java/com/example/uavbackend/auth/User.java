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
  private UserRole role = UserRole.OPERATOR;
  private UserStatus status = UserStatus.ACTIVE;
  private Instant lastLoginAt;
}
