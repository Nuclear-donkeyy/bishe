package com.example.uavbackend.auth;

import com.example.uavbackend.auth.dto.LoginRequest;
import com.example.uavbackend.auth.dto.LoginResponse;
import com.example.uavbackend.auth.dto.UserDto;
import jakarta.transaction.Transactional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final AuthTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${security.jwt.expiration-minutes:120}")
  private long expirationMinutes;

  public Optional<UserDto> findUserByToken(String token) {
    return tokenRepository
        .findByTokenHash(hash(token))
        .filter(t -> t.isActive(Instant.now()))
        .flatMap(t -> userRepository.findById(t.getUserId()))
        .map(this::toDto);
  }

  public Optional<AuthToken> resolveToken(String token) {
    return tokenRepository
        .findByTokenHash(hash(token))
        .filter(t -> t.isActive(Instant.now()));
  }

  @Transactional
  public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
    User user =
        userRepository
            .findByUsername(request.username())
            .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
    if (user.getStatus() == UserStatus.DISABLED) {
      throw new IllegalStateException("账号已禁用");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("账号或密码错误");
    }
    user.setLastLoginAt(Instant.now());

    String tokenValue = UUID.randomUUID().toString().replace("-", "");
    AuthToken token = new AuthToken();
    token.setUserId(user.getId());
    token.setTokenHash(hash(tokenValue));
    token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
    token.setClientIp(clientIp);
    token.setUserAgent(userAgent);
    tokenRepository.save(token);
    return new LoginResponse(tokenValue, toDto(user));
  }

  @Transactional
  public void logout(String token) {
    tokenRepository
        .findByTokenHash(hash(token))
        .ifPresent(
            t -> {
              t.setRevokedAt(Instant.now());
            });
  }

  private String hash(String raw) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(raw.getBytes());
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("无法初始化哈希算法", e);
    }
  }

  private UserDto toDto(User user) {
    return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRole());
  }
}
