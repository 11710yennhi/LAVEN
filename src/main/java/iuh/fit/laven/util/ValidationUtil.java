package iuh.fit.laven.util;

import iuh.fit.laven.exception.AppException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.regex.Pattern;

/** Các kiểm tra định dạng dùng chung cho đăng ký, hồ sơ, tạo nhân viên (theo mục h của tài liệu). */
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    // Chỉ gồm chữ cái (có dấu tiếng Việt) và một dấu cách giữa các từ
    private static final Pattern NAME_PATTERN = Pattern.compile("^\\p{L}+( \\p{L}+)*$");

    private ValidationUtil() {
    }

    /** Trả về email đã chuẩn hóa (cắt khoảng trắng, chữ thường). */
    public static String validateEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw bad("Email không hợp lệ");
        }
        return email;
    }

    public static String validatePhone(String raw) {
        String phone = raw == null ? "" : raw.trim();
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw bad("Số điện thoại không hợp lệ");
        }
        return phone;
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw bad("Mật khẩu không được rỗng");
        }
    }

    public static String validateFullName(String raw) {
        String fullName = raw == null ? "" : raw.trim();
        if (fullName.isEmpty()) {
            throw bad("Họ tên không được rỗng");
        }
        if (fullName.length() > 50) {
            throw bad("Tối đa 50 ký tự");
        }
        if (!NAME_PATTERN.matcher(fullName).matches()) {
            throw bad("Họ tên không hợp lệ");
        }
        return fullName;
    }

    public static LocalDate validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw bad("Ngày sinh không được rỗng");
        }
        if (!dateOfBirth.isBefore(LocalDate.now())) {
            throw bad("Ngày sinh không hợp lệ");
        }
        return dateOfBirth;
    }

    /** Chuỗi tùy chọn: rỗng thì trả về null, ngược lại cắt khoảng trắng. */
    public static String optional(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    /** Chuỗi tùy chọn có giới hạn độ dài. */
    public static String optional(String raw, int maxLength, String errorMessage) {
        String value = optional(raw);
        if (value != null && value.length() > maxLength) {
            throw bad(errorMessage);
        }
        return value;
    }

    private static AppException bad(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }
}
