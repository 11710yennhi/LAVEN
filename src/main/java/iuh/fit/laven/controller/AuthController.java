package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ChangePasswordRequest;
import iuh.fit.laven.dto.request.ForgotPasswordRequest;
import iuh.fit.laven.dto.request.LoginRequest;
import iuh.fit.laven.dto.request.RefreshTokenRequest;
import iuh.fit.laven.dto.request.RegisterRequest;
import iuh.fit.laven.dto.request.ResetPasswordRequest;
import iuh.fit.laven.dto.response.AuthResponse;
import iuh.fit.laven.dto.response.MessageResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new MessageResponse(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản"));
    }

    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verify(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Xác thực email thành công. Bạn có thể đăng nhập"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(new MessageResponse("Đăng xuất thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse(AuthService.FORGOT_PASSWORD_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                          @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse("Đổi mật khẩu thành công. Vui lòng đăng nhập lại"));
    }
}
