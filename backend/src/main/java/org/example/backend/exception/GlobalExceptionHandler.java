package org.example.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<?> handleAppException(AppException ex) {
    return ResponseEntity.status(ex.getStatus())
            .body(Map.of("error", ex.getMessage()));
  }

  // Bắt lỗi xác thực của Spring Security (sai pass, sai user)
  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<?> handleAuthError(Exception ex) {
    return ResponseEntity.status(401)
            .body(Map.of("error", "Xác thực thất bại: " + ex.getMessage()));
  }
}