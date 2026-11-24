package iuh.fit.laven.dto.request.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Body của POST /api/public/ai/chat.
 * Lịch sử do React giữ trong sessionStorage, mỗi lần hỏi gửi kèm ~10 tin gần nhất.
 */
@Getter
@Setter
public class ChatbotRequest {

    @NotBlank(message = "Câu hỏi không được rỗng")
    @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
    private String message;

    /** Sản phẩm khách đang xem (nếu hỏi từ trang chi tiết sản phẩm). */
    @Size(max = 20, message = "Mã sản phẩm không hợp lệ")
    private String productId;

    @Valid
    @Size(max = 50, message = "Lịch sử chat quá dài")
    private List<HistoryMessage> history = new ArrayList<>();

    public enum Role { USER, ASSISTANT }

    public record HistoryMessage(
        @NotNull(message = "Vai trò tin nhắn không được rỗng") Role role,
        @NotBlank(message = "Nội dung tin nhắn không được rỗng")
        @Size(max = 2000, message = "Mỗi tin nhắn tối đa 2000 ký tự") String content
    ) {}
}
