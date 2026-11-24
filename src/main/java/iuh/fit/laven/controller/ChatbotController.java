package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ai.ChatbotRequest;
import iuh.fit.laven.dto.response.ai.ChatbotResponse;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.ai.ChatbotService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chatbot tư vấn: khách chưa đăng nhập cũng hỏi được (/api/public/** đã permitAll).
 * Có gửi JWT thì JwtAuthenticationFilter vẫn chạy, nên principal là tài khoản đang đăng nhập.
 */
@RestController
@RequestMapping(value = "/api/public/ai/chat", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ChatbotResponse chat(@Valid @RequestBody ChatbotRequest request,
                                @AuthenticationPrincipal CustomUserDetails user,
                                HttpServletRequest httpRequest) {
        return chatbotService.chat(request, user, httpRequest);
    }

    @GetMapping("/quota")
    public ChatbotResponse quota(@AuthenticationPrincipal CustomUserDetails user,
                                 HttpServletRequest httpRequest) {
        return chatbotService.quota(user, httpRequest);
    }
}
