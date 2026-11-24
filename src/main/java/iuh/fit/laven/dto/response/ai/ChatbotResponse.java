package iuh.fit.laven.dto.response.ai;

import iuh.fit.laven.dto.response.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Kết quả chatbot. answer đã bỏ dòng [[SP: ...]], React lưu thẳng vào lịch sử được. */
@Getter
@AllArgsConstructor
public class ChatbotResponse {
    private final String answer;

    /** Thẻ sản phẩm AI gợi ý (đọc lại từ DB, có ảnh bìa). */
    private final List<ProductResponse> suggestedProducts;

    /** Số câu còn được hỏi hôm nay. */
    private final int remainingQuestions;

    /** true = khách chưa đăng nhập (giới hạn thấp hơn). */
    private final boolean guest;
}
