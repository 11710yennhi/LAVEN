package iuh.fit.laven.dto.request.order;

import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Bộ lọc danh sách phiếu trả hàng / giao thất bại của Staff. Bỏ trống trường nào thì không lọc theo trường đó. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffReturnFilterRequest {
    private ReturnStatus status;
    private ReturnType returnType;
    private ReturnReason returnReason;
    private String orderId;
    /** Tìm theo tên khách hàng hoặc tên người nhận */
    private String customerName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
    private Integer minRefundAmount;
    private Integer maxRefundAmount;
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 10;
}
