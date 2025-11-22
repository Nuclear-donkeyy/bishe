package com.example.uavbackend.auth;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
  Optional<AuthToken> findByTokenHash(String tokenHash);

  long deleteByUserId(Long userId);

  long deleteByExpiresAtBefore(Instant instant);
}
