package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CategoryProductStatusRequest;
import iuh.fit.laven.dto.response.CategoryProductPage;
import iuh.fit.laven.dto.response.CategoryProductPage.Brand;
import iuh.fit.laven.dto.response.CategoryProductPage.Counts;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class CategoryProductService {
    private static final int PAGE_SIZE = 10;

    private final CategoryService categories;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public CategoryProductPage list(String categoryId, String brand, String status, int page) {
        if (page < 0 || page > Integer.MAX_VALUE / PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số trang không hợp lệ");
        }
        validateStatusFilter(status);
        String brandFilter = normalizeBrand(brand);
        boolean categoryActive = categories.findById(categoryId, true).isStatus();
        long total = 0;
        long matching = 0;
        long selling = 0;
        var brands = new HashMap<String, String>();
        for (var group : products.countCategoryProducts(categoryId, brandFilter)) {
            long count = group.getProductCount();
            total += count;
            if (!group.getBrandKey().isBlank()) {
                brands.putIfAbsent(group.getBrandKey(), group.getBrandLabel());
            }
            matching += group.getMatchingCount();
            if (categoryActive && group.getStatus()) selling += group.getMatchingCount();
        }
        var brandOptions = new ArrayList<Brand>();
        brands.forEach((key, label) -> brandOptions.add(new Brand(key, label)));
        var collator = java.text.Collator.getInstance(Locale.forLanguageTag("vi"));
        brandOptions.sort((a, b) -> collator.compare(a.label(), b.label()));
        var counts = new Counts(matching, selling, matching - selling, 0);

        if (!categoryActive && status.equals("selling")) {
            return new CategoryProductPage(total, counts, brandOptions, List.of(), null);
        }
        Boolean statusFilter = databaseStatus(status, categoryActive);
        var slice = products.findCategoryProducts(categoryId, brandFilter, statusFilter, PageRequest.of(page, PAGE_SIZE));
        return new CategoryProductPage(total, counts, brandOptions, slice.getContent(),
            slice.hasNext() ? page + 1 : null);
    }

    @Transactional
    public int updateStatus(String categoryId, CategoryProductStatusRequest request) {
        var category = categories.findById(categoryId, true);
        boolean target = request.status();
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (!target && reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do ngừng bán");
        }
        if (target && !category.isStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại sản phẩm đang ngừng kinh doanh");
        }

        List<Product> selected;
        if (request.allMatching()) {
            String status = request.filterStatus() == null ? "all" : request.filterStatus();
            validateStatusFilter(status);
            List<Product> matching = !category.isStatus() && status.equals("selling") ? List.of()
                : products.findCategoryProductsForStatusUpdate(categoryId, normalizeBrand(request.brand()),
                    databaseStatus(status, category.isStatus()));
            Set<String> excluded = request.excludedProductIds() == null ? Set.of() : new HashSet<>(request.excludedProductIds());
            selected = matching.stream().filter(product -> !excluded.contains(product.getProductId())).toList();
            if (matching.size() - selected.size() != excluded.size()
                    || request.expectedCount() == null || selected.size() != request.expectedCount()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Kết quả lọc đã thay đổi. Hãy tải lại danh sách rồi chọn lại");
            }
        } else {
            if (request.productIds() == null || request.productIds().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một sản phẩm");
            }
            var ids = new HashSet<>(request.productIds());
            selected = products.findSelectedProductsForStatusUpdate(categoryId, ids);
            if (selected.size() != ids.size()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Có sản phẩm đã chuyển loại hoặc không còn tồn tại");
            }
        }

        int updated = 0;
        for (Product product : selected) {
            if (product.isStatus() == target) continue;
            product.setStatus(target);
            product.setStopSellingReason(target ? null : reason);
            updated++;
        }
        return updated;
    }

    private String normalizeBrand(String brand) {
        return brand == null || brand.isBlank() ? null : brand.strip().toLowerCase(Locale.ROOT);
    }

    private void validateStatusFilter(String status) {
        if (!List.of("all", "selling", "stopped").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái lọc không hợp lệ");
        }
    }

    private Boolean databaseStatus(String status, boolean categoryActive) {
        return switch (status) {
            case "selling" -> Boolean.TRUE;
            case "stopped" -> categoryActive ? Boolean.FALSE : null;
            default -> null;
        };
    }
}
