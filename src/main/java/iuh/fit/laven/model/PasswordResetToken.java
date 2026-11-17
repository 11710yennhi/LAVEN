package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    private static final Pattern HEX64_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    @Id
    @Column(length = 64, nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken(String tokenHash, User user, LocalDateTime expiresAt, LocalDateTime createdAt) {
        setTokenHash(tokenHash);
        setUser(user);
        setExpiresAt(expiresAt);
        setCreatedAt(createdAt);
    }

    public void setTokenHash(String tokenHash) {
        if (tokenHash == null || !HEX64_PATTERN.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ");
        }
        this.tokenHash = tokenHash;
    }

    public void setUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không được rỗng");
        }
        this.user = user;
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

    @Override
    public String toString() {
        return "PasswordResetToken{tokenHash='" + tokenHash + "', userId='"
                + (user == null ? null : user.getUserId()) + "', expiresAt=" + expiresAt
                + ", createdAt=" + createdAt + "}";
    }
}