package iuh.fit.laven.dto.request.order;

import iuh.fit.laven.model.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPreviewRequest {

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<Item> items;
    @NotBlank(message = "Thành phố nhận hàng không được để trống")
    private String receiverCity;
    private String orderPromotionCode;
    private String shippingPromotionCode;
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        @NotBlank(message = "Variant ID không được để trống")
        private String variantId;
        @NotBlank(message = "Color ID không được để trống")
        private String colorId;
        @NotNull(message = "Số lượng không được để trống")
        @Positive(message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}