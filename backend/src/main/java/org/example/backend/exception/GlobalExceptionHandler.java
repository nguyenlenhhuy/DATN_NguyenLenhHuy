package org.example.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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

  // 1. Bắt tất cả các AppException tự định nghĩa
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

  // 4. Bắt ResponseStatusException (Giải quyết lỗi trùng email bị nuốt mất tin nhắn cụ thể)
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
    // Trích xuất mã trạng thái HTTP gốc (ví dụ: 400)
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    // ex.getReason() sẽ lấy chính xác chuỗi "Email này đã tồn tại trong hệ thống LuxeHotel!"
    String message = ex.getReason();

    return buildErrorResponse(status, message);
  }

  // 5. Bắt lỗi vi phạm ràng buộc dữ liệu trực tiếp từ Database (Dự phòng trường hợp lỗi UNIQUE từ DB)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String message = "Dữ liệu không hợp lệ hoặc đã tồn tại trong hệ thống.";
    if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate entry")) {
      message = "Email hoặc số điện thoại này đã được sử dụng. Vui lòng chọn thông tin khác!";
    }
    return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  // 6. Bắt các lỗi Runtime chưa xác định (Lưới lọc cuối cùng)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneralException(Exception ex) {
    // In chi tiết lỗi ra Console để hỗ trợ quá trình kiểm thử ngầm
    System.err.println("=== LỖI HỆ THỐNG CHƯA XÁC ĐỊNH ===");
    ex.printStackTrace();
    System.err.println("==================================");

    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định.");
  }
}