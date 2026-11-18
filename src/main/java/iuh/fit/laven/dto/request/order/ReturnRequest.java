package iuh.fit.laven.dto.request.order;

import iuh.fit.laven.model.enums.ReturnReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @NotNull
    private ReturnReason returnReason;

    @Size(max = 500)
    private String note;

    @Valid
    private List<ReturnItemRequest> items;

    private String videoUrl;

    private List<String> imageUrls;

    private String refundBankName;

    private String refundAccountNumber;

    private String refundAccountName;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnItemRequest {

        @NotBlank
        private String variantId;

        @NotBlank
        private String colorId;

        @NotNull
        @Positive
        private Integer quantity;
    }
}
