package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.ChangePasswordRequest;
import iuh.fit.laven.dto.request.ForgotPasswordRequest;
import iuh.fit.laven.dto.request.LoginRequest;
import iuh.fit.laven.dto.request.RefreshTokenRequest;
import iuh.fit.laven.dto.request.RegisterRequest;
import iuh.fit.laven.dto.request.ResetPasswordRequest;
import iuh.fit.laven.dto.response.AuthResponse;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Cart;
import iuh.fit.laven.model.PasswordResetToken;
import iuh.fit.laven.model.PendingRegistration;
import iuh.fit.laven.model.RefreshToken;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.UserRole;
import iuh.fit.laven.repository.CartRepository;
import iuh.fit.laven.repository.PasswordResetTokenRepository;
import iuh.fit.laven.repository.PendingRegistrationRepository;
import iuh.fit.laven.repository.RefreshTokenRepository;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.security.JwtService;
import iuh.fit.laven.util.IdGenerator;
import iuh.fit.laven.util.TokenHasher;
import iuh.fit.laven.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int VERIFY_EXPIRE_MINUTES = 30;
    private static final int RESET_EXPIRE_MINUTES = 30;
    private static final int REFRESH_EXPIRE_DAYS = 7;
    private static final int MAX_FAILED_LOGIN = 5;
    private static final int LOGIN_LOCK_MINUTES = 15;

    private static final String WRONG_CREDENTIALS = "Email hoặc mật khẩu không đúng";
    private static final String INVALID_VERIFY_LINK = "Link xác thực không hợp lệ hoặc đã hết hạn";
    private static final String INVALID_RESET_LINK = "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn";
    private static final String INVALID_SESSION = "Phiên đăng nhập không hợp lệ hoặc đã hết hạn";
    public static final String FORGOT_PASSWORD_MESSAGE =
            "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu";

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    // =====================================================================
    // UC04: Đăng ký (bước 1-4)
    // =====================================================================
    @Transactional
    public void register(RegisterRequest request) {
        String email = ValidationUtil.validateEmail(request.getEmail());
        String phone = ValidationUtil.validatePhone(request.getPhone());
        ValidationUtil.validatePassword(request.getPassword());
        String fullName = ValidationUtil.validateFullName(request.getFullName());
        LocalDate dateOfBirth = ValidationUtil.validateDateOfBirth(request.getDateOfBirth());
        String gender = ValidationUtil.optional(request.getGender());

        if (userRepository.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        // Xóa bản chờ cũ của email này, băm mật khẩu, sinh token, lưu bản chờ mới
        pendingRegistrationRepository.deleteByEmail(email);
        pendingRegistrationRepository.flush();

        String token = TokenHasher.generateToken();
        PendingRegistration pending = PendingRegistration.builder()
                .tokenHash(TokenHasher.hash(token))
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .expiresAt(LocalDateTime.now().plusMinutes(VERIFY_EXPIRE_MINUTES))
                .build();
        pendingRegistrationRepository.save(pending);

        emailService.sendVerificationEmail(email, token);
    }

    // =====================================================================
    // UC04: Xác thực email (bước 5-6) -> tạo User CUSTOMER + Cart rỗng
    // =====================================================================
    @Transactional
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_VERIFY_LINK);
        }
        PendingRegistration pending = pendingRegistrationRepository.findById(TokenHasher.hash(token.trim()))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, INVALID_VERIFY_LINK));
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_VERIFY_LINK);
        }
        // Kiểm tra lại email chưa có trong users
        if (userRepository.existsByEmail(pending.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        User user = User.builder()
                .userId(IdGenerator.nextUnique("USR", userRepository::existsById))
                .email(pending.getEmail())
                .phone(pending.getPhone())
                .passwordHash(pending.getPasswordHash())
                .fullName(pending.getFullName())
                .role(UserRole.CUSTOMER)
                .dateOfBirth(pending.getDateOfBirth())
                .gender(pending.getGender())
                .build(); // status = true, pendingLock = false, failedLoginAttempts = 0 (mặc định trong entity)
        userRepository.save(user);

        Cart cart = Cart.builder()
                .cartId(IdGenerator.nextUnique("CRT", cartRepository::existsById))
                .customer(user)
                .build();
        cartRepository.save(cart);

        pendingRegistrationRepository.delete(pending);
    }

    // =====================================================================
    // UC05: Đăng nhập
    // noRollbackFor: vẫn lưu failedLoginAttempts / loginLockedUntil dù ném AppException
    // =====================================================================
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String password = request.getPassword() == null ? "" : request.getPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, WRONG_CREDENTIALS));

        LocalDateTime now = LocalDateTime.now();
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã nhập sai quá 5 lần, vui lòng thử lại sau 15 phút");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            if (attempts >= MAX_FAILED_LOGIN) {
                user.setLoginLockedUntil(now.plusMinutes(LOGIN_LOCK_MINUTES));
                attempts = 0;
            }
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            throw new AppException(HttpStatus.UNAUTHORIZED, WRONG_CREDENTIALS);
        }

        // Đúng mật khẩu
        user.setFailedLoginAttempts(0);
        user.setLoginLockedUntil(null);
        userRepository.save(user);

        if (!user.isStatus()) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Tài khoản đã bị khóa. Lý do: " + user.getLockedReason());
        }
        return issueTokens(user);
    }

    // =====================================================================
    // UC05: Làm mới phiên (xoay vòng refresh token)
    // =====================================================================
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String raw = request.getRefreshToken();
        if (raw == null || raw.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, INVALID_SESSION);
        }
        RefreshToken old = refreshTokenRepository.findById(TokenHasher.hash(raw.trim()))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, INVALID_SESSION));

        if (old.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(old);
            throw new AppException(HttpStatus.UNAUTHORIZED, INVALID_SESSION);
        }
        User user = old.getUser();
        if (!user.isStatus()) {
            refreshTokenRepository.deleteByUser(user);
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }
        refreshTokenRepository.delete(old);
        return issueTokens(user);
    }

    // =====================================================================
    // UC07: Đăng xuất (xóa RefreshToken tương ứng; không báo lỗi nếu token không tồn tại)
    // =====================================================================
    @Transactional
    public void logout(RefreshTokenRequest request) {
        String raw = request.getRefreshToken();
        if (raw == null || raw.isBlank()) {
            return;
        }
        refreshTokenRepository.findById(TokenHasher.hash(raw.trim()))
                .ifPresent(refreshTokenRepository::delete);
    }

    // =====================================================================
    // UC06: Quên mật khẩu - luôn trả cùng một thông báo
    // =====================================================================
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = ValidationUtil.validateEmail(request.getEmail());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return; // không tiết lộ email có tồn tại hay không
        }
        passwordResetTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.flush();

        String token = TokenHasher.generateToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenHash(TokenHasher.hash(token))
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(RESET_EXPIRE_MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(email, token);
    }

    // =====================================================================
    // UC06: Đặt lại mật khẩu bằng token trong link email
    // =====================================================================
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String raw = request.getToken();
        if (raw == null || raw.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_RESET_LINK);
        }
        PasswordResetToken resetToken = passwordResetTokenRepository.findById(TokenHasher.hash(raw.trim()))
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, INVALID_RESET_LINK));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(HttpStatus.BAD_REQUEST, INVALID_RESET_LINK);
        }
        ValidationUtil.validatePassword(request.getNewPassword());

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        refreshTokenRepository.deleteByUser(user);
    }

    // =====================================================================
    // UC16: Đổi mật khẩu (đang đăng nhập)
    // =====================================================================
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        String current = request.getCurrentPassword() == null ? "" : request.getCurrentPassword();
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
        ValidationUtil.validatePassword(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
    }

    // =====================================================================
    // UC38: Dọn token và đăng ký hết hạn (gọi từ TokenCleanupJob)
    // =====================================================================
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        long pending = pendingRegistrationRepository.deleteByExpiresAtBefore(now);
        long reset = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        long refresh = refreshTokenRepository.deleteByExpiresAtBefore(now);
        log.info("Dọn token hết hạn: {} đăng ký chờ, {} token đặt lại mật khẩu, {} refresh token",
                pending, reset, refresh);
    }

    // ---------------------------------------------------------------------
    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = TokenHasher.generateToken();
        RefreshToken entity = RefreshToken.builder()
                .tokenHash(TokenHasher.hash(refreshToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_EXPIRE_DAYS))
                .build();
        refreshTokenRepository.save(entity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(UserResponse.from(user))
                .build();
    }
}
