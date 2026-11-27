package iuh.fit.laven.dto.request.order;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

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
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
}
