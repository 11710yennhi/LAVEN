package iuh.fit.laven.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DashboardSalesResponse(LocalDate from, LocalDate to, long orderCount, long netRevenue,
                                     Long averageOrderValue, List<BestSeller> bestSellers,
                                     List<CategoryRevenue> categories) {
    public record BestSeller(String productId, String productName, String categoryName,
                             String imageUrl, long netQuantity) {}

    public record CategoryRevenue(String categoryId, String categoryName, long netRevenue) {}
}
