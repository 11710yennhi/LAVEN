package iuh.fit.laven.service.ai;

import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.SalePriceRange;
import iuh.fit.laven.dto.response.ai.SemanticSearchResponse;
import iuh.fit.laven.dto.response.ai.SemanticSearchResponse.Item;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.VariantRepository;
import iuh.fit.laven.service.ProductImageService;
import iuh.fit.laven.service.ProductService;
import iuh.fit.laven.service.PromotionService;
import iuh.fit.laven.service.PromotionService.ScopedPromotion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tìm kiếm ngữ nghĩa và gợi ý sản phẩm tương tự.
 * Vector chỉ dùng để tìm ra productId; giá, tồn kho, trạng thái, khuyến mãi luôn đọc lại từ DB.
 */
@Slf4j
@Service
public class SemanticSearchService {

    private static final int DEFAULT_TOP_K = 10;
    private static final int DEFAULT_SIMILAR_TOP_K = 4;
    private static final int MAX_TOP_K = 20;

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ColorStockRepository colorStockRepository;
    private final VariantRepository variantRepository;
    private final ProductService productService;
    private final ProductImageService productImageService;
    private final PromotionService promotionService;
    private final ProductDocumentMapper mapper;
    private final ProductIndexService indexService;
    private final double minSimilarity;

    public SemanticSearchService(VectorStore vectorStore,
                                 ProductRepository productRepository,
                                 ColorStockRepository colorStockRepository,
                                 VariantRepository variantRepository,
                                 ProductService productService,
                                 ProductImageService productImageService,
                                 PromotionService promotionService,
                                 ProductDocumentMapper mapper,
                                 ProductIndexService indexService,
                                 @Value("${app.ai.min-similarity:0.55}") double minSimilarity) {
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.colorStockRepository = colorStockRepository;
        this.variantRepository = variantRepository;
        this.productService = productService;
        this.productImageService = productImageService;
        this.promotionService = promotionService;
        this.mapper = mapper;
        this.indexService = indexService;
        this.minSimilarity = minSimilarity;
    }

    // ===================== Hàm đã thống nhất với TV2 (tool chatbot gọi) =====================

    public List<ProductResponse> search(String query, String categoryId,
                                        Integer minPrice, Integer maxPrice, int topK) {
        return searchWithScore(query, categoryId, minPrice, maxPrice, topK).getItems().stream()
                .map(Item::getProduct)
                .toList();
    }

    public List<ProductResponse> similar(String productId, int topK) {
        return similarWithScore(productId, topK).stream()
                .map(Item::getProduct)
                .toList();
    }

    // ===================== Hàm cho controller (có điểm, giá sau khuyến mãi, tồn kho) =====================

    public SemanticSearchResponse searchWithScore(String query, String categoryId,
                                                  Integer minPrice, Integer maxPrice, Integer topK) {
        String text = query == null ? "" : query.trim();
        if (text.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Nội dung tìm kiếm không được rỗng");
        }
        validatePriceRange(minPrice, maxPrice);
        int limit = normalizeTopK(topK, DEFAULT_TOP_K);
        String category = blankToNull(categoryId);

        // Lấy dư gấp 3 vì sau đó còn lọc khoảng giá và bỏ sản phẩm ngừng bán
        Map<String, Double> scores = vectorSearch(text, category, limit * 3, Set.of());
        List<Item> items = toItems(scores, minPrice, maxPrice, limit, Map.of());
        if (!items.isEmpty()) {
            return new SemanticSearchResponse(text, false, items);
        }

        // Fallback: chưa có chỉ mục, lỗi AI hoặc không có kết quả đủ giống → tìm từ khóa hiện có
        return new SemanticSearchResponse(text, true, keywordSearch(text, category, minPrice, maxPrice, limit));
    }

    public List<Item> similarWithScore(String productId, Integer topK) {
        int limit = normalizeTopK(topK, DEFAULT_SIMILAR_TOP_K);
        Product product = productService.findById(productId, false); // 404 nếu không có hoặc ngừng bán

        // Dùng chính đoạn văn mô tả của sản phẩm làm câu truy vấn, loại bỏ chính nó
        String text = mapper.toDocument(product).getText();
        Map<String, Double> scores = vectorSearch(text, null, limit + 1, Set.of(product.getProductId()));
        var covers = productImageService.findCoverUrls(List.copyOf(scores.keySet()));
        List<Item> items = toItems(scores, null, null, limit, covers);
        if (!items.isEmpty()) {
            return items;
        }

        // Fallback: sản phẩm đang bán cùng danh mục
        List<ScopedPromotion> actives = promotionService.loadActiveProductPromotions();
        var candidates = productService.search(null, product.getCategory().getCategoryId(),
                        null, null, null, null, null, null, false).stream()
                .filter(p -> !p.getProductId().equals(product.getProductId()))
                .limit(limit)
                .toList();
        var fallbackCovers = productImageService.findCoverUrls(candidates.stream().map(Product::getProductId).toList());
        return candidates.stream()
                .map(p -> toItem(p, null, actives, null, null, fallbackCovers.get(p.getProductId())))
                .filter(Objects::nonNull)
                .toList();
    }

    // ===================== Xử lý bên trong =====================

    /** Tìm vector gần nhất, trả về productId → điểm, giữ thứ tự giống nhất trước. */
    private Map<String, Double> vectorSearch(String query, String categoryId, int limit, Set<String> excludeIds) {
        if (!indexService.isConfigured()) {
            return Map.of();
        }
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .similarityThreshold(minSimilarity);
            if (categoryId != null) {
                builder.filterExpression(new FilterExpressionBuilder().eq("categoryId", categoryId).build());
            }

            Map<String, Double> scores = new LinkedHashMap<>();
            for (Document document : vectorStore.similaritySearch(builder.build())) {
                Object id = document.getMetadata().get("productId");
                if (id != null && !excludeIds.contains(id.toString())) {
                    scores.putIfAbsent(id.toString(), document.getScore() == null ? 0.0 : document.getScore());
                }
            }
            return scores;
        } catch (RuntimeException e) {
            log.warn("Tìm kiếm vector lỗi, chuyển sang tìm từ khóa: {}", e.getMessage());
            return Map.of();
        }
    }

    /** Đọc lại sản phẩm từ DB, tính giá sau khuyến mãi, rồi mới lọc khoảng giá. */
    private List<Item> toItems(Map<String, Double> scores, Integer minPrice, Integer maxPrice, int limit,
                               Map<String, String> covers) {
        if (scores.isEmpty()) {
            return List.of();
        }
        Map<String, Product> products = productRepository.findAllById(scores.keySet()).stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        List<ScopedPromotion> actives = promotionService.loadActiveProductPromotions();

        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            Product product = products.get(entry.getKey());
            // Bỏ qua sản phẩm đã ngừng bán sau lần index gần nhất
            if (product == null || !ProductIndexService.isSelling(product)) {
                continue;
            }
            Item item = toItem(product, entry.getValue(), actives, minPrice, maxPrice, covers.get(product.getProductId()));
            if (item != null) {
                items.add(item);
            }
            if (items.size() == limit) {
                break;
            }
        }
        return items;
    }

    private List<Item> keywordSearch(String query, String categoryId,
                                     Integer minPrice, Integer maxPrice, int limit) {
        List<ScopedPromotion> actives = promotionService.loadActiveProductPromotions();
        return productService.search(query, categoryId, null, null, null, null, null, null, false).stream()
                .map(p -> toItem(p, null, actives, minPrice, maxPrice, null))
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    /**
     * Giá nằm ở Variant: tính giá cuối (sau khuyến mãi) của từng size, chỉ giữ size nằm trong khoảng giá.
     * Trả null nếu không có size nào khớp (sản phẩm bị loại).
     * Truy vấn nạp sẵn sản phẩm và danh mục để biến thể tính giá bán.
     */
    private Item toItem(Product product, Double score, List<ScopedPromotion> actives,
                        Integer minPrice, Integer maxPrice, String imageUrl) {
        List<Integer> salePrices = new ArrayList<>();
        List<Integer> finalPrices = new ArrayList<>();
        variantRepository.findByProductProductId(product.getProductId()).stream()
                .map(variant -> variant.getSalePrice())
                .filter(Objects::nonNull)
                .forEach(salePrice -> {
                    int discount = promotionService.findBestProductPromotion(product, salePrice, actives)
                            .map(PromotionService.AppliedPromotion::discount)
                            .orElse(0);
                    int finalPrice = salePrice - discount;
                    salePrices.add(salePrice);
                    if (inPriceRange(finalPrice, minPrice, maxPrice)) {
                        finalPrices.add(finalPrice);
                    }
                });
        if (finalPrices.isEmpty()) {
            return null;
        }
        // Tồn kho quản lý theo biến thể–màu: cộng tất cả dòng color_stocks của sản phẩm
        int stock = colorStockRepository.findByVariantProductProductId(product.getProductId()).stream()
                .mapToInt(ColorStock::getStock)
                .sum();
        SalePriceRange range = new SalePriceRange(
                salePrices.stream().min(Integer::compare).orElse(null),
                salePrices.stream().max(Integer::compare).orElse(null));
        return new Item(new ProductResponse(product, range, imageUrl),
                finalPrices.stream().min(Integer::compare).orElseThrow(),
                finalPrices.stream().max(Integer::compare).orElseThrow(),
                stock, score);
    }

    private boolean inPriceRange(int price, Integer minPrice, Integer maxPrice) {
        return (minPrice == null || price >= minPrice) && (maxPrice == null || price <= maxPrice);
    }

    private void validatePriceRange(Integer minPrice, Integer maxPrice) {
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)
                || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng giá không hợp lệ");
        }
    }

    private int normalizeTopK(Integer topK, int defaultValue) {
        if (topK == null) {
            return defaultValue;
        }
        if (topK < 1 || topK > MAX_TOP_K) {
            throw new AppException(HttpStatus.BAD_REQUEST, "topK phải từ 1 đến " + MAX_TOP_K);
        }
        return topK;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}