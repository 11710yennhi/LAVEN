package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "pending_registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingRegistration {

    private static final Pattern HEX64_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]+$");

    @Id
    @Column(length = 64, nullable = false)
    private String tokenHash;

    @Column(length = 100, nullable = false)
    private String email;

    @Column(length = 10, nullable = false)
    private String phone;

    @Column(length = 60, nullable = false)
    private String passwordHash;

    @Column(length = 50, nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== 3.1 Constructor đầy đủ thuộc tính =====
    public PendingRegistration(String tokenHash, String email, String phone, String passwordHash,
                               String fullName, LocalDate dateOfBirth, String gender,
                               LocalDateTime expiresAt, LocalDateTime createdAt) {
        setTokenHash(tokenHash);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        setFullName(fullName);
        setDateOfBirth(dateOfBirth);
        setGender(gender);
        setExpiresAt(expiresAt);
        setCreatedAt(createdAt);
    }

    // ===== 2. Setter có kiểm tra (mục h, bảng PendingRegistration) =====

    public void setTokenHash(String tokenHash) {
        if (tokenHash == null || !HEX64_PATTERN.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ");
        }
        this.tokenHash = tokenHash;
    }

    public void setEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        this.email = email.trim().toLowerCase();
    }

    public void setPhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
        this.phone = phone.trim();
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được rỗng");
        }
        this.passwordHash = passwordHash;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được rỗng");
        }
        String value = fullName.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("Tối đa 50 ký tự");
        }
        if (!NAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Họ tên không hợp lệ");
        }
        this.fullName = value;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Ngày sinh không được rỗng");
        }
        if (!dateOfBirth.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ");
        }
        this.dateOfBirth = dateOfBirth;
    }

    public void setGender(String gender) {
        this.gender = gender == null || gender.isBlank() ? null : gender.trim();
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("Thời điểm hết hạn không được rỗng");
        }
        this.expiresAt = expiresAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Thời điểm tạo không được rỗng");
        }
        this.createdAt = createdAt;
    }

    // ===== 4. toString (không in passwordHash) =====
    @Override
    public String toString() {
        return "PendingRegistration{tokenHash='" + tokenHash + "', email='" + email + "', phone='" + phone
                + "', fullName='" + fullName + "', dateOfBirth=" + dateOfBirth + ", gender='" + gender
                + "', expiresAt=" + expiresAt + ", createdAt=" + createdAt + "}";
    }
}