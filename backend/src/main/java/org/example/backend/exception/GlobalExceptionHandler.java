package org.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // Helper method tạo cấu trúc JSON lỗi thống nhất
  private ResponseEntity<Object> buildErrorResponse(HttpStatus status, String message) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return new ResponseEntity<>(body, status);
  }

  // 1. Bắt tất cả các AppException (bao gồm cả ResourceNotFound vì nó kế thừa từ đây)
  @ExceptionHandler(AppException.class)
  public ResponseEntity<Object> handleAppException(AppException ex) {
    return buildErrorResponse(ex.getStatus(), ex.getMessage());
  }

  // 2. Bắt lỗi Validation (Khi dùng @Valid ở Controller)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  // 3. Bắt lỗi xác thực Spring Security
  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<Object> handleAuthError(Exception ex) {
    return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Xác thực thất bại: " + ex.getMessage());
  }

  // 4. Bắt các lỗi Runtime chưa xác định (Lưới lọc cuối cùng)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception ex) {
    // Lưu ý: Đi làm nên log ex.printStackTrace() ở đây để Debug
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định.");
  }
}