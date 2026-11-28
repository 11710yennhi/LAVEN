package iuh.fit.laven.dto.response;

import java.util.List;

public record CategoryProductPage(
    long total, Counts counts, List<Brand> brands, List<Item> products, Integer nextPage
) {
    public record Counts(long all, long selling, long stopped, long draft) {}
    public record Brand(String key, String label) {}
    public record Item(String productId, String productName, String brand, boolean status, String stopSellingReason) {}
}
