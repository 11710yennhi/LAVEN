package iuh.fit.laven.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategoryProductStatusRequest(
    boolean allMatching,
    @Size(max = 2000) List<@NotBlank @Size(max = 20) String> productIds,
    @Size(max = 2000) List<@NotBlank @Size(max = 20) String> excludedProductIds,
    @Size(max = 100) String brand,
    String filterStatus,
    @NotNull Boolean status,
    @Size(max = 500) String reason,
    @Min(1) @Max(2147483647) Integer expectedCount
) {}
