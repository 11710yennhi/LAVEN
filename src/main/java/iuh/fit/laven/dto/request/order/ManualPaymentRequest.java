package iuh.fit.laven.dto.request.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Nhân viên nhập đúng dữ liệu đã đối chiếu trên sao kê ngân hàng. */
@Getter
@Setter
public class ManualPaymentRequest {
    @NotBlank(message = "Cần nhập mã giao dịch trên sao kê")
    @Size(max = 93)
    private String bankReference;

    @NotBlank
    private String accountNumber;

    @NotNull
    @Positive
    private Long amountIn;

    @NotBlank
    private String transferContent;

    @NotNull
    private LocalDateTime transactionDate;
}
