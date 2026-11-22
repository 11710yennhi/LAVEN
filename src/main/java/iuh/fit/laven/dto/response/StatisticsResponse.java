package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.ReturnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** UC31 - Thống kê (quy trình 17). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private int year;
    private int month;

    /** Doanh thu đã chốt tháng M = Σ (totalAmount − refundAmount) của đơn có finalizedAt trong M */
    private long finalizedRevenue;
    private long finalizedOrderCount;
    /** Tổng tiền hoàn đã trừ trong doanh thu đã chốt tháng M */
    private long finalizedRefundAmount;

    /** Doanh thu tạm tính = Σ totalAmount của đơn COMPLETED chưa chốt sổ (mọi thời điểm), chỉ để tham khảo */
    private long provisionalRevenue;
    private long provisionalOrderCount;

    /** Sản phẩm bán chạy trong các đơn chốt sổ tháng M: Σ (số mua − số trả) */
    private List<BestSeller> bestSellers;

    /** Biến thể-màu sắp hết hàng (stock ≤ 5) của sản phẩm đang bán */
    private List<LowStock> lowStocks;

    /** Số đơn theo trạng thái hiện tại (toàn hệ thống) */
    private Map<OrderStatus, Long> orderCountByStatus;

    /** Chi phí ship chiều về phát sinh trong tháng M theo loại phiếu */
    private List<ReturnCost> returnCosts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestSeller {
        private String productId;
        private String productName;
        private long soldQuantity;
        private long returnedQuantity;
        private long netQuantity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStock {
        private String productId;
        private String productName;
        private String variantId;
        private String size;
        private String colorId;
        private String colorName;
        private int stock;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnCost {
        private ReturnType returnType;
        private long recordCount;
        private long totalReturnShippingCost;
    }
}
