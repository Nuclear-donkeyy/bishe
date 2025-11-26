package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_tokens")
public class AuthToken extends BaseEntity {
  private Long userId;
  private String tokenHash;
  private Instant expiresAt;
  private Instant revokedAt;
  private String clientIp;
  private String userAgent;

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
