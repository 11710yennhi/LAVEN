package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.ChatSessionResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.FirebaseChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Chat dùng phiên JWT LAVEN hiện tại, không yêu cầu đăng nhập lần thứ hai. */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final FirebaseChatService firebaseChatService;

    @GetMapping("/session")
    public ChatSessionResponse session(@AuthenticationPrincipal CustomUserDetails user) {
        return firebaseChatService.createSession(user.getUserId());
    }
}
