package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.dto.LoginRequest;
import com.example.uavbackend.auth.dto.LoginResponse;
import com.example.uavbackend.auth.dto.UserDto;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserMapper userMapper;
  private final AuthTokenMapper tokenMapper;
  private final PasswordEncoder passwordEncoder;

  @Value("${security.jwt.expiration-minutes:120}")
  private long expirationMinutes;

  public Optional<UserDto> findUserByToken(String token) {
    String tokenHash = hash(token);
    Instant now = Instant.now();
    AuthToken authToken =
        tokenMapper.selectOne(
            new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getTokenHash, tokenHash));
    if (authToken == null || !authToken.isActive(now)) {
      return Optional.empty();
    }
    User user = userMapper.selectById(authToken.getUserId());
    return Optional.ofNullable(user).map(this::toDto);
  }

  public Optional<AuthToken> resolveToken(String token) {
    String tokenHash = hash(token);
    AuthToken authToken =
        tokenMapper.selectOne(
            new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getTokenHash, tokenHash));
    if (authToken == null || !authToken.isActive(Instant.now())) {
      return Optional.empty();
    }
    return Optional.of(authToken);
  }

  @Transactional
  public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
    User user =
        Optional.ofNullable(
                userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, request.username())))
            .orElseThrow(() -> new IllegalArgumentException("账户或密码错误"));
    if (user.getStatus() == UserStatus.DISABLED) {
      throw new IllegalStateException("账户已禁用");
    }
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new IllegalArgumentException("账户或密码错误");
    }
    user.setLastLoginAt(Instant.now());
    userMapper.updateById(user);

    String tokenValue = UUID.randomUUID().toString().replace("-", "");
    AuthToken token = new AuthToken();
    token.setUserId(user.getId());
    token.setTokenHash(hash(tokenValue));
    token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
    token.setClientIp(clientIp);
    token.setUserAgent(userAgent);
    tokenMapper.insert(token);
    return new LoginResponse(tokenValue, toDto(user));
  }

  @Transactional
  public void logout(String token) {
    String tokenHash = hash(token);
    AuthToken authToken =
        tokenMapper.selectOne(
            new LambdaQueryWrapper<AuthToken>().eq(AuthToken::getTokenHash, tokenHash));
    if (authToken != null) {
      authToken.setRevokedAt(Instant.now());
      tokenMapper.updateById(authToken);
    }
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
