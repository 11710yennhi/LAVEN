package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQrResponse {
    private String orderId;
    private String bankName;
    private String bankAccount;
    private String accountName;
    private Integer amount;
    private String transferContent;
    private String qrCodeUrl;
    private PaymentStatus paymentStatus;
}
