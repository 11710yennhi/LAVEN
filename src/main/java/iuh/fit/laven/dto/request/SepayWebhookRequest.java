package iuh.fit.laven.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Các trường SePay gửi khi tài khoản ngân hàng có giao dịch. */
@Getter
@Setter
public class SepayWebhookRequest {
    private String id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String code;
    private String content;
    private String transferType;
    private Long transferAmount;
    private String description;
}
