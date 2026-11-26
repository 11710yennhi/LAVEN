package iuh.fit.laven.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ProductImportRequest(
    @NotNull UUID batchId,
    @NotEmpty @Size(max = 50) List<@NotNull Row> rows
) {
    public record Row(
        int line, String productName, String categoryId, String brand, String origin,
        String material, String description, String waterResistance, String wheel,
        String zipper, String warranty, Integer maxQuantityPerOrder,
        Boolean status, String stopSellingReason
    ) {}
}
