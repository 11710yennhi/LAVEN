package iuh.fit.laven.service.ai;

import iuh.fit.laven.dto.request.ai.ChatbotRequest;
import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.SalePriceRange;
import iuh.fit.laven.dto.response.ai.ChatbotResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.ChatUsage;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.ChatUsageRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.ProductImageService;
import iuh.fit.laven.service.ProductService;
import iuh.fit.laven.util.TokenHasher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chatbot tư vấn vali/túi/balo. Backend không lưu hội thoại: React gửi kèm lịch sử mỗi lần hỏi.
 * Không đặt @Transactional: lời gọi Gemini có thể mất vài giây, không giữ kết nối DB trong lúc chờ.
 */
@Slf4j
@Service
public class ChatbotService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_HISTORY_CHARS = 12_000;
    private static final String GLOBAL_KEY = "ALL";
    private static final Pattern PRODUCT_MARKER = Pattern.compile("\\[\\[\\s*SP\\s*:\\s*([A-Za-z0-9]+)\\s*]]");

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý tư vấn của cửa hàng LAVEN, chuyên vali, túi và balo. Luôn trả lời bằng tiếng Việt, ngắn gọn, thân thiện.

        Quy tắc:
        1. Chỉ tư vấn chọn sản phẩm: kích thước, sức chứa, quy định hành lý máy bay, chất liệu, công năng,
           kiểu dáng, trọng lượng, giá, cách dùng và bảo quản. Không tư vấn chính sách đổi trả, bảo hành,
           giao hàng, thanh toán hay khuyến mãi; với các câu đó, mời khách xem trang chính sách hoặc chat với nhân viên.
           Câu hỏi ngoài chủ đề thì lịch sự từ chối.
        2. Mọi giá, tồn kho, kích thước và quy định hãng bay PHẢI lấy từ tool trong lượt này.
           Lịch sử hội thoại có thể không chính xác: không dùng lại số liệu trong lịch sử, hãy gọi tool lại.
           Không bịa sản phẩm, giá hay quy định. Tool không có dữ liệu thì nói rõ là chưa có.
        3. Chọn tool:
           - Hỏi một sản phẩm cụ thể có mang lên máy bay / ký gửi được không → checkAirlineBaggage.
           - Cần gợi ý sản phẩm để đi máy bay (xách tay hoặc ký gửi) → suggestLuggageForTrip.
             Nếu chưa rõ xách tay hay ký gửi, hỏi lại khách trước khi gọi.
           - Gợi ý theo nhu cầu khác (chất liệu, công năng, kiểu dáng, nhẹ, bền...) → findProductsByNeed.
        4. Khi nói về quy định hãng bay, nhắc khách quy định có thể thay đổi và đưa link nguồn từ tool.
        5. Với mỗi sản phẩm bạn giới thiệu, thêm một dòng riêng ở cuối câu trả lời theo đúng dạng [[SP: <mã sản phẩm>]].
           Chỉ dùng mã sản phẩm do tool trả về trong lượt này. Tối đa 5 dòng.
        """;

    private final ChatClient chatClient;
    private final LuggageAdvisorTools tools;
    private final ChatUsageRepository chatUsageRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ProductImageService productImageService;
    private final String apiKey;
    private final int guestLimit;
    private final int userLimit;
    private final int globalLimit;
    private final boolean trustProxy;

    public ChatbotService(ChatClient.Builder chatClientBuilder,
                          LuggageAdvisorTools tools,
                          ChatUsageRepository chatUsageRepository,
                          ProductRepository productRepository,
                          ProductService productService,
                          ProductImageService productImageService,
                          @Value("${spring.ai.google.genai.api-key:}") String apiKey,
                          @Value("${app.ai.chat.guest-limit:3}") int guestLimit,
                          @Value("${app.ai.chat.user-limit:10}") int userLimit,
                          @Value("${app.ai.chat.global-limit:200}") int globalLimit,
                          @Value("${app.ai.chat.trust-proxy:false}") boolean trustProxy) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.tools = tools;
        this.chatUsageRepository = chatUsageRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.productImageService = productImageService;
        this.apiKey = apiKey;
        this.guestLimit = guestLimit;
        this.userLimit = userLimit;
        this.globalLimit = globalLimit;
        this.trustProxy = trustProxy;
    }

    // ===================== API =====================

    public ChatbotResponse chat(ChatbotRequest request, CustomUserDetails user, HttpServletRequest httpRequest) {
        // 1. Chưa cấu hình Gemini thì báo lỗi ngay, không trừ lượt
        if (apiKey == null || apiKey.isBlank() || "missing-key".equals(apiKey)) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Chatbot chưa được cấu hình");
        }
        boolean guest = user == null;
        String subjectKey = subjectKey(user, httpRequest);
        int limit = guest ? guestLimit : userLimit;
        LocalDate today = LocalDate.now();

        // 2. Trừ lượt toàn hệ thống trước, rồi lượt của người hỏi; hết lượt cá nhân thì hoàn lượt hệ thống
        if (!reserve(GLOBAL_KEY, today, globalLimit)) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS,
                "Chatbot đã hết lượt hỏi hôm nay, bạn vui lòng quay lại vào ngày mai");
        }
        if (!reserve(subjectKey, today, limit)) {
            release(GLOBAL_KEY, today);
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, guest
                ? "Bạn đã dùng hết " + limit + " câu hỏi hôm nay. Đăng nhập để hỏi thêm"
                : "Bạn đã dùng hết " + limit + " câu hỏi hôm nay");
        }

        // 3. Gọi Gemini; lỗi thì hoàn cả 2 lượt
        Set<String> suggestedIds = Collections.synchronizedSet(new LinkedHashSet<>());
        String answer;
        try {
            answer = chatClient.prompt()
                .messages(sanitizeHistory(request.getHistory()))
                .user(buildQuestion(request))
                .tools(tools)
                .toolContext(Map.of(LuggageAdvisorTools.SUGGESTED_IDS, suggestedIds))
                .call()
                .content();
        } catch (RuntimeException e) {
            log.warn("Gọi Gemini lỗi: {}", e.getMessage());
            release(subjectKey, today);
            release(GLOBAL_KEY, today);
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Chatbot đang bận, bạn vui lòng thử lại sau");
        }

        // 4. Chỉ hiện thẻ của sản phẩm AI nhắc tới VÀ do tool trả về trong lượt này
        String text = answer == null ? "" : answer;
        List<ProductResponse> products = suggestedProducts(text, suggestedIds);
        String cleanAnswer = PRODUCT_MARKER.matcher(text).replaceAll("").strip();
        return new ChatbotResponse(cleanAnswer, products, remaining(subjectKey, today, limit), guest);
    }

    /** Số câu còn được hỏi hôm nay (để React hiện "Bạn còn 2 câu hỏi"). */
    public ChatbotResponse quota(CustomUserDetails user, HttpServletRequest httpRequest) {
        boolean guest = user == null;
        int limit = guest ? guestLimit : userLimit;
        int remaining = remaining(subjectKey(user, httpRequest), LocalDate.now(), limit);
        return new ChatbotResponse(null, List.of(), remaining, guest);
    }

    /** Job dọn dẹp: xóa số liệu đếm lượt cũ hơn 7 ngày. */
    public int cleanupOldUsages() {
        return chatUsageRepository.deleteByUsageDateBefore(LocalDate.now().minusDays(7));
    }

    // ===================== Đếm lượt hỏi =====================

    private boolean reserve(String subjectKey, LocalDate date, int limit) {
        String usageId = usageId(subjectKey, date);
        chatUsageRepository.createIfAbsent(usageId, subjectKey, date);
        return chatUsageRepository.increaseIfUnder(usageId, limit) == 1;
    }

    private void release(String subjectKey, LocalDate date) {
        chatUsageRepository.decrease(usageId(subjectKey, date));
    }

    /** Còn lại = ít hơn giữa lượt cá nhân và lượt toàn hệ thống. */
    private int remaining(String subjectKey, LocalDate date, int limit) {
        int used = countOf(subjectKey, date);
        int globalUsed = countOf(GLOBAL_KEY, date);
        return Math.max(0, Math.min(limit - used, globalLimit - globalUsed));
    }

    private int countOf(String subjectKey, LocalDate date) {
        return chatUsageRepository.findById(usageId(subjectKey, date))
            .map(ChatUsage::getQuestionCount)
            .orElse(0);
    }

    private String usageId(String subjectKey, LocalDate date) {
        return subjectKey + "|" + date;
    }

    /** Tài khoản đếm theo userId; khách chưa đăng nhập đếm theo IP đã băm (không lưu IP thật). */
    private String subjectKey(CustomUserDetails user, HttpServletRequest request) {
        return user != null ? "U:" + user.getUserId() : "G:" + TokenHasher.hash(clientIp(request));
    }

    /**
     * Chỉ tin X-Forwarded-For khi chạy sau proxy (trust-proxy=true), và lấy IP CUỐI
     * vì đó là IP do proxy của mình thêm vào; các IP phía trước khách tự sửa được.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (!parts[i].isBlank()) {
                        return parts[i].trim();
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }

    // ===================== Xây prompt =====================

    /**
     * Lịch sử do client gửi nên không tin hoàn toàn:
     * giữ 10 tin cuối và tối đa 12.000 ký tự, bỏ tin ASSISTANT ở đầu,
     * gộp các tin liền nhau cùng vai trò, bỏ tin USER ở cuối (câu hỏi trước đó chưa có trả lời),
     * xóa các dòng [[SP: ...]] để khách không cài sẵn mã sản phẩm.
     */
    private List<Message> sanitizeHistory(List<ChatbotRequest.HistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<ChatbotRequest.HistoryMessage> recent = new ArrayList<>(
            history.subList(Math.max(0, history.size() - MAX_HISTORY_MESSAGES), history.size()));

        // Bỏ bớt từ tin cũ nhất cho đến khi tổng ký tự không vượt giới hạn
        int total = recent.stream().mapToInt(m -> m.content() == null ? 0 : m.content().length()).sum();
        while (!recent.isEmpty() && total > MAX_HISTORY_CHARS) {
            ChatbotRequest.HistoryMessage removed = recent.remove(0);
            total -= removed.content() == null ? 0 : removed.content().length();
        }

        List<ChatbotRequest.Role> roles = new ArrayList<>();
        List<StringBuilder> contents = new ArrayList<>();
        for (ChatbotRequest.HistoryMessage message : recent) {
            if (message.role() == null || message.content() == null) {
                continue;
            }
            String content = PRODUCT_MARKER.matcher(message.content()).replaceAll("").strip();
            if (content.isEmpty() || (roles.isEmpty() && message.role() == ChatbotRequest.Role.ASSISTANT)) {
                continue;
            }
            if (!roles.isEmpty() && roles.get(roles.size() - 1) == message.role()) {
                contents.get(contents.size() - 1).append('\n').append(content);
            } else {
                roles.add(message.role());
                contents.add(new StringBuilder(content));
            }
        }
        // Câu hỏi hiện tại là tin USER kế tiếp, nên lịch sử phải kết thúc bằng ASSISTANT
        if (!roles.isEmpty() && roles.get(roles.size() - 1) == ChatbotRequest.Role.USER) {
            roles.remove(roles.size() - 1);
            contents.remove(contents.size() - 1);
        }

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < roles.size(); i++) {
            String content = contents.get(i).toString();
            messages.add(roles.get(i) == ChatbotRequest.Role.USER
                ? new UserMessage(content)
                : new AssistantMessage(content));
        }
        return messages;
    }

    /** Thêm thông tin sản phẩm khách đang xem để AI biết "vali này" là vali nào. */
    private String buildQuestion(ChatbotRequest request) {
        String question = request.getMessage().strip();
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            return question;
        }
        return productRepository.findById(request.getProductId().trim())
            .filter(ProductIndexService::isSelling)
            .map(p -> "(Khách đang xem sản phẩm mã " + p.getProductId() + ": " + p.getProductDisplayName() + ")\n"
                + question)
            .orElse(question);
    }

    // ===================== Thẻ sản phẩm =====================

    private List<ProductResponse> suggestedProducts(String answer, Set<String> suggestedIds) {
        List<String> ids = new ArrayList<>();
        Matcher matcher = PRODUCT_MARKER.matcher(answer);
        while (matcher.find() && ids.size() < 5) {
            String id = matcher.group(1);
            if (suggestedIds.contains(id) && !ids.contains(id)) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllById(ids).stream()
            .filter(ProductIndexService::isSelling)
            .sorted((a, b) -> Integer.compare(ids.indexOf(a.getProductId()), ids.indexOf(b.getProductId())))
            .toList();
        Map<String, SalePriceRange> ranges = productService.findSalePriceRanges(products);
        Map<String, String> covers = productImageService.findCoverUrls(ids);
        return products.stream()
            .map(p -> new ProductResponse(p, ranges.get(p.getProductId()), covers.get(p.getProductId())))
            .toList();
    }
}
