package iuh.fit.laven.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import iuh.fit.laven.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Tạo và đọc access token JWT (HS256, mặc định 60 phút): subject = userId, claim role, iat, exp. */
@Service
public class JwtService {

    /** Claim đánh dấu loại token đặc biệt. Access token KHÔNG có claim này. */
    public static final String PURPOSE_CLAIM = "purpose";
    public static final String EMAIL_CHANGE_PURPOSE = "email_change";
    public static final String OLD_EMAIL_CLAIM = "oldEmail";
    public static final String NEW_EMAIL_CLAIM = "newEmail";
    public static final long EMAIL_CHANGE_EXPIRATION_MINUTES = 30;

    private final SecretKey key;
    private final long accessExpirationMinutes;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.access-expiration-minutes:60}") long accessExpirationMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Thiếu hoặc quá ngắn JWT_SECRET: cần ít nhất 32 ký tự, khai báo trong file .env");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMinutes = accessExpirationMinutes;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUserId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Kiểm tra chữ ký và hạn; token sai hoặc hết hạn thì ném JwtException. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Token trong link xác thực đổi email (không lưu DB): subject = userId,
     * kèm email cũ, email mới, hạn 30 phút. Ký bằng cùng JWT_SECRET nên không sửa được nội dung.
     */
    public String generateEmailChangeToken(User user, String newEmail) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUserId())
                .claim(PURPOSE_CLAIM, EMAIL_CHANGE_PURPOSE)
                .claim(OLD_EMAIL_CLAIM, user.getEmail())
                .claim(NEW_EMAIL_CLAIM, newEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EMAIL_CHANGE_EXPIRATION_MINUTES, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Đọc token đổi email; sai chữ ký, hết hạn hoặc không phải token đổi email thì ném JwtException. */
    public Claims parseEmailChangeToken(String token) {
        Claims claims = parse(token);
        if (!EMAIL_CHANGE_PURPOSE.equals(claims.get(PURPOSE_CLAIM, String.class))) {
            throw new JwtException("Không phải token đổi email");
        }
        return claims;
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMinutes * 60;
    }
}
