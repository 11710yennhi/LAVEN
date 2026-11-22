package iuh.fit.laven.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Mã tham chiếu trên sao kê mà nhân viên đã đối chiếu với đơn. */
@Getter
@Setter
public class ManualPaymentRequest {
    @NotBlank(message = "Cần nhập mã giao dịch trên sao kê")
    @Size(max = 100)
    private String bankReference;
}
