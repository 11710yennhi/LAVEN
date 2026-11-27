package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ChangeEmailRequest;
import iuh.fit.laven.dto.request.UpdateProfileRequest;
import iuh.fit.laven.dto.response.MessageResponse;
import iuh.fit.laven.dto.response.UserResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hồ sơ cá nhân (UC15) cho mọi role đã đăng nhập. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUserId()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUserId(), request));
    }

    /** Đổi email (mọi role): gửi link xác thực tới email mới; email chỉ đổi khi bấm link. */
    @PostMapping("/me/email")
    public ResponseEntity<MessageResponse> requestEmailChange(@AuthenticationPrincipal CustomUserDetails principal,
                                                              @RequestBody ChangeEmailRequest request) {
        userService.requestEmailChange(principal.getUserId(), request);
        return ResponseEntity.ok(new MessageResponse(
                "Đã gửi link xác thực tới email mới. Vui lòng kiểm tra hộp thư (link có hiệu lực 30 phút)"));
    }
}
