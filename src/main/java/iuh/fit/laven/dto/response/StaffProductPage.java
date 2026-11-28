package iuh.fit.laven.dto.response;

import java.util.List;

public record StaffProductPage(List<StaffProductResponse> products, long total, Integer nextPage) {}
