package iuh.fit.laven.exception;

import iuh.fit.laven.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Lỗi nghiệp vụ do service chủ động ném
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    // Lỗi kiểm tra dữ liệu từ setter của Model (tài liệu mục i.2: IllegalArgumentException -> 400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // JSON sai cú pháp hoặc sai kiểu (ví dụ ngày sinh không đúng dạng yyyy-MM-dd)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không hợp lệ");
    }

    // Thiếu tham số trên URL (vd quên ?colorId=) hoặc sai kiểu (vd chữ ở chỗ cần số)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadParameter(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Thiếu hoặc sai tham số yêu cầu");
    }

    // Gõ sai đường dẫn API
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn yêu cầu");
    }

    // Đúng đường dẫn nhưng sai method (vd dùng GET cho API chỉ nhận POST)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ cho đường dẫn này");
    }
    // Service của TV1 ném ResponseStatusException: giữ đúng mã và câu thông báo TV1 đã đặt
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return build(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
    }

    // @Valid trên DTO sai dữ liệu (MethodArgumentNotValidException là lớp con của BindException)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex) {
        String message = ex.getFieldErrors().isEmpty() ? "Dữ liệu gửi lên không hợp lệ"
                : ex.getFieldErrors().get(0).getDefaultMessage();
        return build(HttpStatus.BAD_REQUEST, message);
    }
    // Lỗi không lường trước: ghi log, không lộ chi tiết cho client
    @ExceptionHandler(Exception.class)

    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Lỗi không xác định", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}