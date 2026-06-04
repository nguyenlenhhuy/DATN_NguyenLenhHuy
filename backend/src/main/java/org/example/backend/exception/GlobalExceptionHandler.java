package org.example.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException; // ✅ ĐÃ SỬA THÀNH SERVER

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(AppException.class)
  public ResponseEntity<Object> handleAppException(AppException ex) {
    return buildErrorResponse(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<Object> handleAuthError(Exception ex) {
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Xác thực thất bại: " + ex.getMessage());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    return buildErrorResponse(status, ex.getReason());
  }

  // Bắt lỗi trùng lịch phòng (Biến đổi từ lỗi 500 thành 400 sạch sẽ)
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
    return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String message = "Dữ liệu không hợp lệ hoặc đã tồn tại trong hệ thống.";
    if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate entry")) {
      message = "Email hoặc số điện thoại này đã được sử dụng. Vui lòng chọn thông tin khác!";
    }
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception ex) {
    System.err.println("=== LỖI HỆ THỐNG CHƯA XÁC ĐỊNH ===");
    ex.printStackTrace();
    System.err.println("==================================");
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định.");
  }
}