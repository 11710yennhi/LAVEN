package iuh.fit.laven.service.ai;

import iuh.fit.laven.dto.response.ai.SemanticSearchResponse;
import iuh.fit.laven.model.Airline;
import iuh.fit.laven.model.BaggageRule;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.model.enums.BaggageType;
import iuh.fit.laven.repository.AirlineRepository;
import iuh.fit.laven.repository.BaggageRuleRepository;
import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.VariantRepository;
import iuh.fit.laven.service.PromotionService;
import iuh.fit.laven.service.PromotionService.ScopedPromotion;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 3 tool cho chatbot. Mọi số liệu (kích thước, giá, tồn kho, quy định) đọc từ DB lúc gọi.
 * Tool trả về văn bản để Gemini đọc; mã sản phẩm đã trả về được ghi vào ToolContext
 * ("suggestedIds") để ChatbotService chỉ hiện thẻ của sản phẩm thật sự do tool tìm ra.
 */
@Component
public class LuggageAdvisorTools {

    public static final String SUGGESTED_IDS = "suggestedIds";
    private static final int MAX_RESULTS = 5;

    private final AirlineRepository airlineRepository;
    private final BaggageRuleRepository baggageRuleRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final ColorStockRepository colorStockRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionService promotionService;
    private final SemanticSearchService semanticSearchService;

    public LuggageAdvisorTools(AirlineRepository airlineRepository,
                               BaggageRuleRepository baggageRuleRepository,
                               ProductRepository productRepository,
                               VariantRepository variantRepository,
                               ColorStockRepository colorStockRepository,
                               CategoryRepository categoryRepository,
                               PromotionService promotionService,
                               SemanticSearchService semanticSearchService) {
        this.airlineRepository = airlineRepository;
        this.baggageRuleRepository = baggageRuleRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.colorStockRepository = colorStockRepository;
        this.categoryRepository = categoryRepository;
        this.promotionService = promotionService;
        this.semanticSearchService = semanticSearchService;
    }

    // ===================== Tool 1 =====================

    @Tool(description = """
        Kiểm tra từng size của một sản phẩm có đúng quy định hành lý xách tay và ký gửi của một hãng bay không.
        Dùng khi khách hỏi sản phẩm cụ thể có mang lên máy bay / ký gửi được không.""")
    public String checkAirlineBaggage(
        @ToolParam(description = "Mã sản phẩm, ví dụ PRO2026...") String productId,
        @ToolParam(description = "Tên hãng bay, ví dụ Vietjet, Vietnam Airlines, Bamboo") String airline,
        @ToolParam(required = false, description = "Hạng vé nếu khách có nói, ví dụ Eco, Phổ thông") String fareClass,
        ToolContext toolContext
    ) {
        Product product = productId == null ? null : productRepository.findById(productId.trim()).orElse(null);
        if (product == null || !ProductIndexService.isSelling(product)) {
            return "Không tìm thấy sản phẩm đang bán với mã " + productId + ".";
        }
        Optional<Airline> found = findAirline(airline);
        if (found.isEmpty()) {
            return "Chưa có dữ liệu quy định hành lý của hãng \"" + airline + "\". Các hãng đang có dữ liệu: "
                + activeAirlineNames() + ".";
        }
        Airline air = found.get();
        List<BaggageRule> rules = baggageRuleRepository
            .findByAirlineAirlineIdOrderByBaggageTypeAscFareClassAsc(air.getAirlineId()).stream()
            .filter(rule -> fareClass == null || normalize(fareClass).isEmpty()
                || normalize(rule.getFareClass()).contains(normalize(fareClass)))
            .toList();
        if (rules.isEmpty()) {
            return "Hãng " + air.getAirlineName() + " chưa có quy định cho hạng vé \"" + fareClass + "\".";
        }
        List<Variant> variants = variantRepository.findByProductProductId(product.getProductId());
        if (variants.isEmpty()) {
            return "Sản phẩm " + product.getProductDisplayName() + " chưa có size nào.";
        }
        addSuggested(toolContext, product.getProductId());

        StringBuilder text = new StringBuilder();
        text.append("Sản phẩm ").append(product.getProductId()).append(" - ").append(product.getProductDisplayName())
            .append(", hãng ").append(air.getAirlineName()).append(":\n");
        for (BaggageRule rule : rules) {
            text.append("* ").append(typeName(rule.getBaggageType())).append(" hạng ").append(rule.getFareClass())
                .append(" (").append(describeRule(rule)).append("):\n");
            for (Variant variant : variants) {
                text.append("  - Size ").append(variant.getSize()).append(": ")
                    .append(checkVariant(variant, rule)).append('\n');
            }
        }
        text.append(policyNote(air));
        return text.toString();
    }

    // ===================== Tool 2 =====================

    @Tool(description = """
        Gợi ý vali/túi/balo vừa quy định hành lý (xách tay hoặc ký gửi) của hãng bay, còn hàng, trong ngân sách.
        Nếu khách không nói hãng thì chọn sản phẩm vừa với mọi hãng đang có dữ liệu.""")
    public String suggestLuggageForTrip(
        @ToolParam(required = false, description = "Tên hãng bay nếu khách có nói") String airline,
        @ToolParam(description = "CARRY_ON = xách tay lên cabin, CHECKED = ký gửi") BaggageType baggageType,
        @ToolParam(required = false, description = "Ngân sách tối đa (VND) nếu khách có nói") Integer maxBudget,
        ToolContext toolContext
    ) {
        if (baggageType == null) {
            return "Cần biết khách muốn mang xách tay (CARRY_ON) hay ký gửi (CHECKED).";
        }
        Airline air = null;
        List<BaggageRule> rules;
        if (airline == null || airline.isBlank()) {
            rules = baggageRuleRepository.findByAirlineStatusTrueAndBaggageType(baggageType);
        } else {
            Optional<Airline> found = findAirline(airline);
            if (found.isEmpty()) {
                return "Chưa có dữ liệu quy định hành lý của hãng \"" + airline + "\". Các hãng đang có dữ liệu: "
                    + activeAirlineNames() + ".";
            }
            air = found.get();
            rules = baggageRuleRepository.findByAirlineAirlineIdOrderByBaggageTypeAscFareClassAsc(air.getAirlineId())
                .stream().filter(rule -> rule.getBaggageType() == baggageType).toList();
        }
        // Hạng vé có 0 kiện thì không mang được loại hành lý này, không dùng để so
        rules = rules.stream().filter(rule -> rule.getPieceCount() > 0).toList();
        if (rules.isEmpty()) {
            return "Chưa có quy định " + typeName(baggageType).toLowerCase() + " nào để so.";
        }

        // Size phải vừa MỌI quy định đã chọn = vừa quy định chặt nhất, hạng vé nào cũng mang được
        List<ScopedPromotion> actives = promotionService.loadActiveProductPromotions();
        List<Candidate> candidates = new ArrayList<>();
        for (Variant variant : variantRepository.findForTrip()) {
            List<BaggageRule> finalRules = rules;
            if (finalRules.stream().allMatch(rule -> fits(variant, rule))) {
                Integer finalPrice = finalPrice(variant, actives);
                if (finalPrice != null && (maxBudget == null || maxBudget <= 0 || finalPrice <= maxBudget)) {
                    candidates.add(new Candidate(variant, finalPrice));
                }
            }
        }
        // Trong giới hạn của hãng, dung tích lớn hơn có lợi hơn; cùng dung tích thì rẻ hơn trước
        candidates.sort(Comparator
            .comparing((Candidate c) -> c.variant().getCapacity() == null ? 0.0 : c.variant().getCapacity())
            .reversed()
            .thenComparing(Candidate::finalPrice));

        StringBuilder text = new StringBuilder();
        Set<String> chosenProducts = new HashSet<>();
        Map<String, Integer> stockCache = new HashMap<>();
        for (Candidate candidate : candidates) {
            Variant variant = candidate.variant();
            Product product = variant.getProduct();
            if (chosenProducts.contains(product.getProductId()) || variantStock(variant, stockCache) <= 0) {
                continue;
            }
            chosenProducts.add(product.getProductId());
            addSuggested(toolContext, product.getProductId());
            text.append("- ").append(product.getProductId()).append(" - ").append(product.getProductDisplayName())
                .append(", size ").append(variant.getSize())
                .append(", ").append(dimensions(variant))
                .append(variant.getCapacity() == null ? "" : ", " + number(variant.getCapacity()) + " L")
                .append(", giá ").append(money(candidate.finalPrice())).append('\n');
            if (chosenProducts.size() == MAX_RESULTS) {
                break;
            }
        }
        if (chosenProducts.isEmpty()) {
            return "Không có sản phẩm còn hàng vừa quy định " + typeName(baggageType).toLowerCase()
                + (maxBudget == null ? "" : " trong ngân sách " + money(maxBudget)) + ".";
        }
        String header = air == null
            ? "Sản phẩm vừa quy định " + typeName(baggageType).toLowerCase() + " của mọi hãng đang có dữ liệu ("
                + activeAirlineNames() + "):\n"
            : "Sản phẩm vừa quy định " + typeName(baggageType).toLowerCase() + " của " + air.getAirlineName()
                + " (mọi hạng vé):\n";
        return header + text + (air == null ? "" : policyNote(air));
    }

    // ===================== Tool 3 =====================

    @Tool(description = """
        Tìm sản phẩm theo nhu cầu mô tả bằng lời (chất liệu, công năng, kiểu dáng, nhẹ, chống nước, đi công tác...).
        Dùng khi câu hỏi không liên quan quy định hãng bay.""")
    public String findProductsByNeed(
        @ToolParam(description = "Nhu cầu của khách, viết đầy đủ bằng tiếng Việt") String need,
        @ToolParam(required = false, description = "Tên danh mục nếu khách có nói, ví dụ Vali, Balo, Túi") String categoryName,
        @ToolParam(required = false, description = "Ngân sách tối đa (VND) nếu khách có nói") Integer maxBudget,
        ToolContext toolContext
    ) {
        if (need == null || need.isBlank()) {
            return "Cần mô tả nhu cầu của khách.";
        }
        String categoryId = null;
        String key = categoryName == null ? "" : normalize(categoryName);
        if (!key.isEmpty()) {
            categoryId = categoryRepository.findByStatusTrueOrderByCategoryNameAsc().stream()
                .filter(c -> normalize(c.getCategoryName()).contains(key) || key.contains(normalize(c.getCategoryName())))
                .map(Category::getCategoryId)
                .findFirst().orElse(null);
        }
        Integer budget = maxBudget == null || maxBudget <= 0 ? null : maxBudget;
        List<SemanticSearchResponse.Item> items = semanticSearchService
            .searchWithScore(need, categoryId, null, budget, MAX_RESULTS).getItems().stream()
            .filter(item -> item.getStock() != null && item.getStock() > 0)
            .toList();
        if (items.isEmpty()) {
            return "Không tìm thấy sản phẩm còn hàng phù hợp nhu cầu này.";
        }
        StringBuilder text = new StringBuilder("Sản phẩm phù hợp:\n");
        for (SemanticSearchResponse.Item item : items) {
            var p = item.getProduct();
            addSuggested(toolContext, p.getProductId());
            text.append("- ").append(p.getProductId()).append(" - ")
                .append(p.getCategoryName()).append(' ').append(p.getBrand()).append(' ').append(p.getProductName())
                .append(", giá ").append(money(item.getFinalPrice()))
                .append(item.getMaxFinalPrice() != null && !item.getMaxFinalPrice().equals(item.getFinalPrice())
                    ? " - " + money(item.getMaxFinalPrice()) : "")
                .append(", chất liệu ").append(p.getMaterial())
                .append(p.getWheel() == null ? "" : ", bánh xe " + p.getWheel())
                .append(p.getZipper() == null ? "" : ", khóa kéo " + p.getZipper())
                .append(p.getWaterResistance() == null ? "" : ", chống nước " + p.getWaterResistance())
                .append(p.getDescription() == null ? "" : ". Mô tả: " + shorten(p.getDescription(), 300))
                .append('\n');
        }
        return text.toString();
    }

    // ===================== So sánh với quy định =====================

    /** Vali rỗng phải vừa kích thước và nhẹ hơn cân nặng tối đa. */
    private boolean fits(Variant variant, BaggageRule rule) {
        if (variant.getLength() == null || variant.getWidth() == null
            || variant.getHeight() == null || variant.getWeight() == null) {
            return false;
        }
        double[] size = sortedDesc(variant.getLength(), variant.getWidth(), variant.getHeight());
        if (rule.getMaxLength() != null) {
            // Xếp 2 bên từ lớn đến nhỏ rồi so từng chiều, để xoay vali vẫn tính đúng
            double[] limit = sortedDesc(rule.getMaxLength(), rule.getMaxWidth(), rule.getMaxHeight());
            for (int i = 0; i < 3; i++) {
                if (size[i] > limit[i]) {
                    return false;
                }
            }
        }
        if (rule.getMaxTotalDimension() != null && size[0] + size[1] + size[2] > rule.getMaxTotalDimension()) {
            return false;
        }
        return variant.getWeight() < rule.getMaxWeight();
    }

    private String checkVariant(Variant variant, BaggageRule rule) {
        if (rule.getPieceCount() == 0) {
            return "hạng vé này không có " + typeName(rule.getBaggageType()).toLowerCase();
        }
        if (variant.getLength() == null || variant.getWidth() == null
            || variant.getHeight() == null || variant.getWeight() == null) {
            return "chưa có đủ số đo để kiểm tra";
        }
        String info = dimensions(variant) + ", nặng " + number(variant.getWeight()) + " kg";
        if (!fits(variant, rule)) {
            return "KHÔNG ĐẠT (" + info + ")";
        }
        double remain = rule.getMaxWeight() - variant.getWeight();
        return "ĐẠT (" + info + "), còn mang thêm khoảng " + number(remain) + " kg đồ";
    }

    private String describeRule(BaggageRule rule) {
        StringBuilder text = new StringBuilder();
        text.append(rule.getPieceCount()).append(" kiện, tối đa ").append(number(rule.getMaxWeight())).append(" kg");
        if (rule.getMaxLength() != null) {
            text.append(", ").append(number(rule.getMaxLength())).append('x').append(number(rule.getMaxWidth()))
                .append('x').append(number(rule.getMaxHeight())).append(" cm");
        }
        if (rule.getMaxTotalDimension() != null) {
            text.append(", tổng 3 chiều ≤ ").append(number(rule.getMaxTotalDimension())).append(" cm");
        }
        if (rule.getNote() != null) {
            text.append(", ghi chú: ").append(rule.getNote());
        }
        return text.toString();
    }

    // ===================== Hỗ trợ =====================

    private record Candidate(Variant variant, Integer finalPrice) {}

    private Integer finalPrice(Variant variant, List<ScopedPromotion> actives) {
        Integer salePrice = variant.getSalePrice();
        if (salePrice == null) {
            return null;
        }
        int discount = promotionService.findBestProductPromotion(variant.getProduct(), salePrice, actives)
            .map(PromotionService.AppliedPromotion::discount)
            .orElse(0);
        return salePrice - discount;
    }

    /** Tồn kho của một size = cộng các màu; đọc theo sản phẩm và nhớ lại để không truy vấn lặp. */
    private int variantStock(Variant variant, Map<String, Integer> cache) {
        String productId = variant.getProduct().getProductId();
        if (!cache.containsKey(variant.getVariantId())) {
            for (ColorStock stock : colorStockRepository.findByVariantProductProductId(productId)) {
                cache.merge(stock.getId().getVariantId(), stock.getStock(), Integer::sum);
            }
            cache.putIfAbsent(variant.getVariantId(), 0);
        }
        return cache.get(variant.getVariantId());
    }

    /** Tìm hãng đang dùng theo tên, không phân biệt dấu và hoa thường. */
    private Optional<Airline> findAirline(String name) {
        String key = name == null ? "" : normalize(name);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return airlineRepository.findByStatusTrueOrderByAirlineNameAsc().stream()
            .filter(a -> normalize(a.getAirlineName()).contains(key) || key.contains(normalize(a.getAirlineName())))
            .findFirst();
    }

    private String activeAirlineNames() {
        List<String> names = airlineRepository.findByStatusTrueOrderByAirlineNameAsc().stream()
            .map(Airline::getAirlineName).toList();
        return names.isEmpty() ? "chưa có hãng nào" : String.join(", ", names);
    }

    private String policyNote(Airline airline) {
        return "Nguồn: " + airline.getBaggagePolicyUrl() + " (cửa hàng kiểm tra ngày " + airline.getVerifiedAt()
            + "; quy định có thể thay đổi, khách nên xem lại trang của hãng trước khi bay).";
    }

    @SuppressWarnings("unchecked")
    private void addSuggested(ToolContext toolContext, String productId) {
        if (toolContext != null && toolContext.getContext().get(SUGGESTED_IDS) instanceof Set<?> ids) {
            ((Set<String>) ids).add(productId);
        }
    }

    private String typeName(BaggageType type) {
        return type == BaggageType.CARRY_ON ? "Xách tay" : "Ký gửi";
    }

    private String dimensions(Variant variant) {
        return number(variant.getLength()) + "x" + number(variant.getWidth()) + "x" + number(variant.getHeight()) + " cm";
    }

    private double[] sortedDesc(double a, double b, double c) {
        double[] values = {a, b, c};
        Arrays.sort(values);
        return new double[]{values[2], values[1], values[0]};
    }

    private String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    private String money(Integer value) {
        return value == null ? "chưa có giá" : String.format("%,d", value).replace(',', '.') + "đ";
    }

    private String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    /** Bỏ dấu, đổi đ → d, viết thường, để "Viet jet" / "vietjet" / "VietJet Air" so được với nhau. */
    static String normalize(String value) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .replace('đ', 'd').replace('Đ', 'D');
        return text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
