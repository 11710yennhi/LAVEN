package iuh.fit.laven.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import iuh.fit.laven.model.User;
import iuh.fit.laven.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Mỗi request: đọc Bearer token, kiểm tra chữ ký, hạn, status của user
 * và từ chối token có iat trước passwordChangedAt (tài liệu mục c.3 bước 8).
 * Token không hợp lệ thì không đặt Authentication; SecurityConfig sẽ trả 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parse(token);
                userRepository.findById(claims.getSubject()).ifPresent(user -> {
                    if (isTokenAcceptable(user, claims)) {
                        CustomUserDetails principal = new CustomUserDetails(user);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            } catch (JwtException | IllegalArgumentException ex) {
                // Token sai chữ ký, hết hạn hoặc sai định dạng: bỏ qua, request sẽ bị 401
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenAcceptable(User user, Claims claims) {
        if (!user.isStatus()) {
            return false;
        }
        LocalDateTime changedAt = user.getPasswordChangedAt();
        if (changedAt != null && claims.getIssuedAt() != null) {
            // iat của JWT chỉ có độ chính xác giây, nên cắt passwordChangedAt về giây để
            // token cấp ngay sau khi đổi mật khẩu (cùng giây) không bị từ chối nhầm.
            LocalDateTime issuedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(claims.getIssuedAt().getTime()), ZoneId.systemDefault());
            if (issuedAt.isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS))) {
                return false;
            }
        }
        return true;
    }
}
