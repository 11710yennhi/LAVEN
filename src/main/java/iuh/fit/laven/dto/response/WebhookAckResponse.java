package iuh.fit.laven.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** SePay nhận HTTP 200; message giúp kiểm tra kết quả tại ngrok. */
@Getter
@AllArgsConstructor
public class WebhookAckResponse {
    private boolean success;
    private String message;
}
