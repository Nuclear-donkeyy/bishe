package com.example.uavbackend.auth;

import com.example.uavbackend.auth.dto.LoginRequest;
import com.example.uavbackend.auth.dto.LoginResponse;
import com.example.uavbackend.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    LoginResponse response =
        authService.login(
            request,
            servletRequest.getRemoteAddr(),
            servletRequest.getHeader("User-Agent"));
    return ResponseEntity.ok(response);
  }

  @GetMapping("/profile")
  public ResponseEntity<UserDto> profile(@RequestHeader("Authorization") String authorization) {
    String token = authorization.replace("Bearer ", "");
    return authService
        .findUserByToken(token)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(401).build());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
    String token = authorization.replace("Bearer ", "");
    authService.logout(token);
    return ResponseEntity.noContent().build();
  }
}
