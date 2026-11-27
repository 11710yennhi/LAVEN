package iuh.fit.laven.dto.request.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Staff nhận và kiểm tra hàng trả về (UC28, quy trình 11 bước 5 và 12 bước 6).
 * Món không gửi lên được coi là không hư hỏng (damagedQuantity = 0).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiveReturnRequest {

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        /** Món khách đã đặt (A) */
        private String variantId;
        private String colorId;

        /** 0 ≤ damagedQuantity ≤ quantity trả */
        private Integer damagedQuantity;

        /** Bắt buộc khi damagedQuantity > 0 */
        private String inspectionNote;

        /** Chỉ dùng khi WRONG_ITEM: món shop gửi nhầm thực tế nhận về (B), bắt buộc */
        private String receivedVariantId;
        private String receivedColorId;
    }
}
