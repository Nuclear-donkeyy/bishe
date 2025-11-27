package com.example.uavbackend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
    // 业务提示类异常，使用 200 + success=false 方便前端直接展示 message
    return ResponseEntity.ok(ApiError.of(ex.getMessage(), "INVALID_ARGUMENT"));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex) {
    return ResponseEntity.ok(ApiError.of(ex.getMessage(), "INVALID_STATE"));
  }

  public record ApiError(boolean success, String message, String errorCode) {
    public static ApiError of(String message, String errorCode) {
      return new ApiError(false, message, errorCode);
    }
  }
}
