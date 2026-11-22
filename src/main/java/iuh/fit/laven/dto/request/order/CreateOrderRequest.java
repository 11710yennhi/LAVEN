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
public class CreateOrderRequest {

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<Item> items;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
    private String receiverPhone;

    @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
    private String receiverAddressLine;

    @NotBlank(message = "Phường/xã không được để trống")
    private String receiverWard;

    @NotBlank(message = "Tỉnh/thành phố không được để trống")
    private String receiverCity;

    private String orderPromotionCode;
    private String shippingPromotionCode;
    private String note;

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