package iuh.fit.laven.dto.request.order;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderFilterRequest {
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String orderId;
    private String customerName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
    private Integer minTotalAmount;
    private Integer maxTotalAmount;
    /** Đơn chứa ít nhất một trong các sản phẩm này (OR). Gửi productIds=A&productIds=B hoặc productIds=A,B */
    private List<String> productIds;
    /** Thứ tự theo ngày đặt: DESC (mới nhất trước, mặc định) hoặc ASC */
    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.DESC;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
}
