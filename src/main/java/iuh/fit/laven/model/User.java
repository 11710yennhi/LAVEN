package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    // static: không phải thuộc tính, Hibernate không tạo cột cho các biến này
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^USR\\d{16}$");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} ]+$");

    @Id
    @Column(length = 19, nullable = false)
    private String userId;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 10, nullable = false)
    private String phone;

    @Column(length = 60, nullable = false)
    private String passwordHash;

    @Column(length = 50, nullable = false)
    private String fullName;

    @Column(length = 255)
    private String addressLine;

    @Column(length = 100)
    private String ward;

    @Column(length = 100)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean status;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    private LocalDate hireDate;

    private LocalDate lockedDate;

    @Column(length = 255)
    private String lockedReason;

    @Column(nullable = false)
    private boolean pendingLock;

    private LocalDateTime passwordChangedAt;

    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;

    private LocalDateTime loginLockedUntil;

    // ===== 3.1 Constructor đầy đủ thuộc tính =====
    // Thứ tự gọi setter có chủ đích: setRole trước setHireDate, setStatus trước setLockedReason
    public User(String userId, String email, String phone, String passwordHash, String fullName,
                String addressLine, String ward, String city, UserRole role, boolean status,
                LocalDate dateOfBirth, String gender, LocalDate hireDate, LocalDate lockedDate,
                String lockedReason, boolean pendingLock, LocalDateTime passwordChangedAt,
                Integer failedLoginAttempts, LocalDateTime loginLockedUntil) {
        setUserId(userId);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        setFullName(fullName);
        setAddressLine(addressLine);
        setWard(ward);
        setCity(city);
        setRole(role);
        setStatus(status);
        setDateOfBirth(dateOfBirth);
        setGender(gender);
        setHireDate(hireDate);
        setLockedDate(lockedDate);
        setLockedReason(lockedReason);
        setPendingLock(pendingLock);
        setPasswordChangedAt(passwordChangedAt);
        setFailedLoginAttempts(failedLoginAttempts);
        setLoginLockedUntil(loginLockedUntil);
    }

    // ===== 2. Setter có kiểm tra (mục h, bảng User) =====

    public void setUserId(String userId) {
        if (userId == null || !USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("Mã người dùng không hợp lệ");
        }
        this.userId = userId;
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

    public void setAddressLine(String addressLine) {
        this.addressLine = blankToNull(addressLine);
    }

    public void setWard(String ward) {
        this.ward = blankToNull(ward);
    }

    public void setCity(String city) {
        this.city = blankToNull(city);
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }
        this.role = role;
    }

    public void setStatus(boolean status) {
        this.status = status;
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
        this.gender = blankToNull(gender);
    }

    public void setHireDate(LocalDate hireDate) {
        if ((role == UserRole.STAFF || role == UserRole.ADMIN) && hireDate == null) {
            throw new IllegalArgumentException("Ngày vào làm không được rỗng đối với nhân viên");
        }
        this.hireDate = hireDate;
    }

    public void setLockedDate(LocalDate lockedDate) {
        this.lockedDate = lockedDate;
    }

    public void setLockedReason(String lockedReason) {
        if (!status && (lockedReason == null || lockedReason.isBlank())) {
            throw new IllegalArgumentException("Lý do khóa không được rỗng khi khóa tài khoản");
        }
        this.lockedReason = blankToNull(lockedReason);
    }

    public void setPendingLock(boolean pendingLock) {
        this.pendingLock = pendingLock;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        int value = failedLoginAttempts == null ? 0 : failedLoginAttempts;
        if (value < 0) {
            throw new IllegalArgumentException("Số lần đăng nhập sai không hợp lệ");
        }
        this.failedLoginAttempts = value;
    }

    public void setLoginLockedUntil(LocalDateTime loginLockedUntil) {
        this.loginLockedUntil = loginLockedUntil;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // ===== 4. toString (không in passwordHash để tránh lộ ra log) =====
    @Override
    public String toString() {
        return "User{userId='" + userId + "', email='" + email + "', phone='" + phone
                + "', fullName='" + fullName + "', addressLine='" + addressLine + "', ward='" + ward
                + "', city='" + city + "', role=" + role + ", status=" + status
                + ", dateOfBirth=" + dateOfBirth + ", gender='" + gender + "', hireDate=" + hireDate
                + ", lockedDate=" + lockedDate + ", lockedReason='" + lockedReason
                + "', pendingLock=" + pendingLock + ", passwordChangedAt=" + passwordChangedAt
                + ", failedLoginAttempts=" + failedLoginAttempts
                + ", loginLockedUntil=" + loginLockedUntil + "}";
    }
}