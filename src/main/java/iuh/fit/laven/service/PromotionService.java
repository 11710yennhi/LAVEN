package iuh.fit.laven.service;

import iuh.fit.laven.dto.request.CompensationTemplateRequest;
import iuh.fit.laven.dto.request.PromotionRequest;
import iuh.fit.laven.dto.response.PromotionResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Promotion;
import iuh.fit.laven.model.PromotionBrand;
import iuh.fit.laven.model.PromotionBrandId;
import iuh.fit.laven.model.PromotionCategory;
import iuh.fit.laven.model.PromotionCategoryId;
import iuh.fit.laven.model.PromotionProduct;
import iuh.fit.laven.model.PromotionProductId;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.PromotionBrandRepository;
import iuh.fit.laven.repository.PromotionCategoryRepository;
import iuh.fit.laven.repository.PromotionProductRepository;
import iuh.fit.laven.repository.PromotionRepository;
import iuh.fit.laven.util.CodeGenerator;
import iuh.fit.laven.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    public static final String TEMPLATE_CODE = "KM00000000";
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{4,20}$");
    private static final int AUTO_CODE_LENGTH = 8;

    private final PromotionRepository promotionRepository;
    private final PromotionBrandRepository promotionBrandRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    /** Khuyến mãi sản phẩm đã áp dụng và số tiền giảm trên MỘT sản phẩm. */
    public record AppliedPromotion(Promotion promotion, int discount) {
    }

    /** Khuyến mãi PRODUCT kèm phạm vi (brand đã đổi chữ thường) để so khớp nhanh. */
    public record ScopedPromotion(Promotion promotion, Set<String> brands,
                                  Set<String> categoryIds, Set<String> productIds) {
    }

    // =====================================================================
    // 1. CRUD khuyến mãi công khai (Staff) - UC29
    // =====================================================================
    @Transactional(readOnly = true)
    public List<PromotionResponse> listPublic() {
        return promotionRepository.findByCustomerIsNull().stream()
                .filter(p -> !TEMPLATE_CODE.equals(p.getCode()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionResponse getById(String promotionId) {
        return toResponse(find(promotionId));
    }

    @Transactional
    public PromotionResponse createPublic(PromotionRequest request) {
        Promotion p = new Promotion();
        p.setPromotionId(IdGenerator.nextUnique("PRM", promotionRepository::existsById));
        p.setCustomer(null);
        applyRequest(p, request, true);
        promotionRepository.save(p);
        saveScope(p, request);
        return toResponse(p);
    }

    @Transactional
    public PromotionResponse updatePublic(String promotionId, PromotionRequest request) {
        Promotion p = find(promotionId);
        if (p.getCustomer() != null || TEMPLATE_CODE.equals(p.getCode())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ Admin được sửa mã bồi thường và mã mẫu");
        }
        applyRequest(p, request, false);
        promotionRepository.save(p);
        saveScope(p, request);
        return toResponse(p);
    }

    // =====================================================================
    // 2. Khách xem mã của tôi - UC14 (mã công khai còn hạn + mã riêng của khách)
    //    Chưa loại các mã khách đã dùng: cần OrderRepository của phần Đơn hàng.
    // =====================================================================
    @Transactional(readOnly = true)
    public List<PromotionResponse> listForCustomer(String customerId) {
        LocalDate today = LocalDate.now();
        return promotionRepository
                .findByStatusTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today).stream()
                .filter(p -> p.getApplyType() != PromotionApplyType.PRODUCT)
                .filter(p -> !TEMPLATE_CODE.equals(p.getCode()))
                .filter(p -> p.getCustomer() == null || customerId.equals(p.getCustomer().getUserId()))
                .map(this::toResponse)
                .toList();
    }

    // =====================================================================
    // 3. Admin: mã bồi thường và mã mẫu - UC33
    // =====================================================================
    @Transactional(readOnly = true)
    public List<PromotionResponse> listCompensation() {
        return promotionRepository.findByCustomerIsNotNull().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PromotionResponse updateCompensation(String promotionId, PromotionRequest request) {
        Promotion p = find(promotionId);
        if (p.getCustomer() == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy mã bồi thường");
        }
        request.setApplyType(p.getApplyType()); // không đổi loại áp dụng của mã đã cấp
        applyRequest(p, request, false);
        promotionRepository.save(p);
        return toResponse(p);
    }

    @Transactional
    public PromotionResponse getTemplate() {
        return toResponse(ensureCompensationTemplate());
    }

    @Transactional
    public PromotionResponse updateTemplate(CompensationTemplateRequest request) {
        Promotion t = ensureCompensationTemplate();
        if (request.getDiscountType() == null) {
            throw bad("Loại giảm giá không hợp lệ");
        }
        Integer value = request.getDiscountValue();
        if (value == null || value <= 0) {
            throw bad("Giá trị giảm phải lớn hơn 0");
        }
        if (value > 100) {
            throw bad("Phần trăm giảm không được vượt quá 100");
        }
        int max = request.getMaxDiscountValue() == null ? 0 : request.getMaxDiscountValue();
        if (max < 0) {
            throw bad("Giá trị giảm tối đa phải lớn hơn hoặc bằng 0");
        }
        if (max > 100) {
            throw bad("Giá trị giảm tối đa của mã mẫu là phần trăm, không được vượt quá 100");
        }
        t.setDiscountType(request.getDiscountType());
        t.setDiscountValue(value);
        t.setMaxDiscountValue(max);
        promotionRepository.save(t);
        return toResponse(t);
    }

    /** Khởi động: bảo đảm có đúng một mã mẫu KM00000000 (status = false, customer = null). */
    @Transactional
    public Promotion ensureCompensationTemplate() {
        return promotionRepository.findByCode(TEMPLATE_CODE).orElseGet(() -> {
            Promotion t = new Promotion();
            t.setPromotionId(IdGenerator.nextUnique("PRM", promotionRepository::existsById));
            t.setName("Mã mẫu khuyến mãi bồi thường");
            t.setDescription("Mã mẫu để hệ thống cấp mã bồi thường khi Staff hủy đơn. Không dùng trực tiếp.");
            t.setApplyType(PromotionApplyType.ORDER);
            t.setCode(TEMPLATE_CODE);
            t.setDiscountType(DiscountType.FIXED_AMOUNT);
            t.setDiscountValue(10);     // 10% giá trị đơn bị hủy
            t.setMinOrderValue(0);
            t.setMaxDiscountValue(10);  // 10% giá trị đơn bị hủy
            t.setStartDate(LocalDate.now());
            t.setEndDate(LocalDate.of(2099, 12, 31));
            t.setStatus(false);
            t.setCustomer(null);
            t.setMaxOrderUsage(null);
            return promotionRepository.save(t);
        });
    }

    // =====================================================================
    // 4. Cấp mã bồi thường khi Staff hủy đơn - UC34 (phần Đơn hàng sẽ gọi)
    //    base = totalAmount của đơn bị hủy. Chạy trong transaction của việc hủy đơn.
    // =====================================================================
    @Transactional
    public Promotion createCompensationPromotion(User customer, int base) {
        Promotion t = ensureCompensationTemplate();

        Promotion p = new Promotion();
        p.setPromotionId(IdGenerator.nextUnique("PRM", promotionRepository::existsById));
        p.setName("Mã bồi thường đơn hàng bị hủy");
        p.setDescription("Mã bồi thường do cửa hàng hủy đơn. Chỉ áp dụng cho tài khoản này, một đơn hàng.");
        p.setApplyType(PromotionApplyType.ORDER);
        p.setCode(CodeGenerator.nextUnique(AUTO_CODE_LENGTH, c -> promotionRepository.findByCode(c).isPresent()));
        p.setDiscountType(t.getDiscountType());
        p.setMinOrderValue(0);
        if (t.getDiscountType() == DiscountType.PERCENT) {
            p.setDiscountValue(t.getDiscountValue());
            p.setMaxDiscountValue(ceil1000((long) base * t.getMaxDiscountValue() / 100));
        } else {
            int amount = ceil1000((long) base * t.getDiscountValue() / 100);
            p.setDiscountValue(amount);
            p.setMaxDiscountValue(amount);
        }
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusMonths(1));
        p.setStatus(true);
        p.setCustomer(customer);
        p.setMaxOrderUsage(1);
        promotionRepository.save(p);

        emailService.sendCompensationEmail(customer.getEmail(), p.getCode(), p.getEndDate());
        return p;
    }

    // =====================================================================
    // 5. Tính toán (phần Giỏ hàng và Đơn hàng dùng) - công thức phần g
    // =====================================================================

    /** Nạp một lần các khuyến mãi PRODUCT đang hiệu lực kèm phạm vi. */
    @Transactional(readOnly = true)
    public List<ScopedPromotion> loadActiveProductPromotions() {
        LocalDate today = LocalDate.now();
        return promotionRepository
                .findByApplyTypeAndStatusTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        PromotionApplyType.PRODUCT, today, today).stream()
                .map(p -> new ScopedPromotion(p,
                        promotionBrandRepository.findByPromotion_PromotionId(p.getPromotionId()).stream()
                                .map(x -> x.getId().getBrand().toLowerCase()).collect(Collectors.toSet()),
                        promotionCategoryRepository.findByPromotion_PromotionId(p.getPromotionId()).stream()
                                .map(x -> x.getId().getCategoryId()).collect(Collectors.toSet()),
                        promotionProductRepository.findByPromotion_PromotionId(p.getPromotionId()).stream()
                                .map(x -> x.getId().getProductId()).collect(Collectors.toSet())))
                .toList();
    }

    /** Mỗi sản phẩm chỉ lấy khuyến mãi giảm nhiều nhất, không cộng dồn. */
    public Optional<AppliedPromotion> findBestProductPromotion(Product product, List<ScopedPromotion> actives) {
        int salePrice = product.getSalePrice();
        AppliedPromotion best = null;
        for (ScopedPromotion sp : actives) {
            if (!appliesTo(sp, product)) {
                continue;
            }
            int discount = calculateProductDiscount(sp.promotion(), salePrice);
            if (discount > 0 && (best == null || discount > best.discount())) {
                best = new AppliedPromotion(sp.promotion(), discount);
            }
        }
        return Optional.ofNullable(best);
    }

    public Optional<AppliedPromotion> findBestProductPromotion(Product product) {
        return findBestProductPromotion(product, loadActiveProductPromotions());
    }

    /** Giảm trên MỘT sản phẩm. PERCENT có trần maxDiscountValue (> 0); FIXED_AMOUNT không vượt giá bán. */
    public int calculateProductDiscount(Promotion p, int salePrice) {
        long discount;
        if (p.getDiscountType() == DiscountType.PERCENT) {
            discount = (long) salePrice * p.getDiscountValue() / 100;
            if (p.getMaxDiscountValue() != null && p.getMaxDiscountValue() > 0) {
                discount = Math.min(discount, p.getMaxDiscountValue());
            }
        } else {
            discount = Math.min(p.getDiscountValue(), salePrice);
        }
        return (int) discount;
    }

    /** orderDiscount: không có mã thì 0; giảm trên productTotal. */
    public int calculateOrderDiscount(Promotion p, int productTotal) {
        if (p == null) {
            return 0;
        }
        long discount;
        if (p.getDiscountType() == DiscountType.PERCENT) {
            discount = (long) productTotal * p.getDiscountValue() / 100;
            if (p.getMaxDiscountValue() != null && p.getMaxDiscountValue() > 0) {
                discount = Math.min(discount, p.getMaxDiscountValue());
            }
        } else {
            discount = Math.min(p.getDiscountValue(), productTotal);
        }
        return (int) discount;
    }

    /** shippingDiscount: không có mã thì 0; giảm trên shippingFee và không vượt shippingFee. */
    public int calculateShippingDiscount(Promotion p, int shippingFee) {
        if (p == null) {
            return 0;
        }
        long discount;
        if (p.getDiscountType() == DiscountType.PERCENT) {
            discount = (long) shippingFee * p.getDiscountValue() / 100;
            if (p.getMaxDiscountValue() != null && p.getMaxDiscountValue() > 0) {
                discount = Math.min(discount, p.getMaxDiscountValue());
            }
        } else {
            discount = p.getDiscountValue();
        }
        return (int) Math.min(discount, shippingFee);
    }

    /**
     * Kiểm tra mã ORDER/SHIPPING khi đặt hàng (quy trình 7 bước 3), khóa dòng Promotion.
     * CHƯA kiểm tra 2 điều cần đếm đơn: "khách chưa từng dùng mã" và "lượt dùng < maxOrderUsage";
     * phần Đơn hàng kiểm tra bằng OrderRepository, và đặt status = false khi mã đạt maxOrderUsage.
     */
    @Transactional
    public Promotion getValidCodeForOrder(String code, PromotionApplyType expectedType,
                                          String customerId, int productTotal) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        Promotion p = promotionRepository.findByCodeForUpdate(normalized)
                .orElseThrow(() -> bad("Mã giảm giá không tồn tại"));
        if (p.getApplyType() != expectedType) {
            throw bad("Mã giảm giá không áp dụng cho loại này");
        }
        if (!p.isStatus()) {
            throw bad("Mã giảm giá không còn hiệu lực");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(p.getStartDate()) || today.isAfter(p.getEndDate())) {
            throw bad("Mã giảm giá không nằm trong thời gian áp dụng");
        }
        if (productTotal < p.getMinOrderValue()) {
            throw bad("Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này");
        }
        if (p.getCustomer() != null && !p.getCustomer().getUserId().equals(customerId)) {
            throw bad("Mã giảm giá không dành cho tài khoản này");
        }
        return p;
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================
    private boolean appliesTo(ScopedPromotion sp, Product product) {
        if (sp.productIds().contains(product.getProductId())) {
            return true;
        }
        // Có cả brand và category thì phải khớp cả hai (quy trình 13 bước 1)
        if (sp.brands().isEmpty() && sp.categoryIds().isEmpty()) {
            return false;
        }
        boolean brandOk = sp.brands().isEmpty()
                || (product.getBrand() != null && sp.brands().contains(product.getBrand().toLowerCase()));
        boolean categoryOk = sp.categoryIds().isEmpty()
                || sp.categoryIds().contains(product.getCategory().getCategoryId());
        return brandOk && categoryOk;
    }

    private void applyRequest(Promotion p, PromotionRequest r, boolean isNew) {
        String name = r.getName() == null ? "" : r.getName().trim();
        if (name.isEmpty()) {
            throw bad("Tên khuyến mãi không được rỗng");
        }
        if (name.length() > 100) {
            throw bad("Tên khuyến mãi tối đa 100 ký tự");
        }
        String description = r.getDescription() == null || r.getDescription().isBlank()
                ? null : r.getDescription().trim();
        if (description != null && description.length() > 255) {
            throw bad("Mô tả tối đa 255 ký tự");
        }
        PromotionApplyType applyType = r.getApplyType();
        if (applyType == null) {
            throw bad("Loại áp dụng khuyến mãi không hợp lệ");
        }
        DiscountType discountType = r.getDiscountType();
        if (discountType == null) {
            throw bad("Loại giảm giá không hợp lệ");
        }
        Integer discountValue = r.getDiscountValue();
        if (discountValue == null || discountValue <= 0) {
            throw bad("Giá trị giảm phải lớn hơn 0");
        }
        if (discountType == DiscountType.PERCENT && discountValue > 100) {
            throw bad("Phần trăm giảm không được vượt quá 100");
        }
        int minOrderValue = r.getMinOrderValue() == null ? 0 : r.getMinOrderValue();
        if (minOrderValue < 0) {
            throw bad("Giá trị đơn hàng tối thiểu phải lớn hơn hoặc bằng 0");
        }
        int maxDiscountValue = r.getMaxDiscountValue() == null ? 0 : r.getMaxDiscountValue();
        if (maxDiscountValue < 0) {
            throw bad("Giá trị giảm tối đa phải lớn hơn hoặc bằng 0");
        }
        if (r.getStartDate() == null) {
            throw bad("Ngày bắt đầu không được rỗng");
        }
        if (r.getEndDate() == null || r.getEndDate().isBefore(r.getStartDate())) {
            throw bad("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
        Integer maxOrderUsage = r.getMaxOrderUsage();
        if (applyType == PromotionApplyType.PRODUCT) {
            maxOrderUsage = null; // chỉ dùng cho ORDER/SHIPPING
        } else if (maxOrderUsage != null && maxOrderUsage < 1) {
            throw bad("Số lượt sử dụng tối đa phải lớn hơn hoặc bằng 1");
        }

        p.setName(name);
        p.setDescription(description);
        p.setApplyType(applyType);
        p.setCode(resolveCode(p, r, applyType, isNew));
        p.setDiscountType(discountType);
        p.setDiscountValue(discountValue);
        p.setMinOrderValue(minOrderValue);
        p.setMaxDiscountValue(maxDiscountValue);
        p.setStartDate(r.getStartDate());
        p.setEndDate(r.getEndDate());
        p.setMaxOrderUsage(maxOrderUsage);
        if (r.getStatus() != null) {
            p.setStatus(r.getStatus());
        } else if (isNew) {
            p.setStatus(true);
        }
    }

    private String resolveCode(Promotion p, PromotionRequest r, PromotionApplyType applyType, boolean isNew) {
        if (applyType == PromotionApplyType.PRODUCT) {
            return null; // khuyến mãi sản phẩm không có mã
        }
        String code = r.getCode() == null ? "" : r.getCode().trim().toUpperCase();
        if (code.isEmpty()) {
            if (!isNew) {
                throw bad("Mã giảm giá không được rỗng đối với khuyến mãi đơn hàng/vận chuyển");
            }
            return CodeGenerator.nextUnique(AUTO_CODE_LENGTH,
                    c -> promotionRepository.findByCode(c).isPresent());
        }
        if (!CODE_PATTERN.matcher(code).matches() || TEMPLATE_CODE.equals(code)) {
            throw bad("Mã giảm giá không hợp lệ");
        }
        Optional<Promotion> existing = promotionRepository.findByCode(code);
        if (existing.isPresent() && !existing.get().getPromotionId().equals(p.getPromotionId())) {
            throw new AppException(HttpStatus.CONFLICT, "Mã giảm giá đã tồn tại");
        }
        return code;
    }

    /** Ghi lại phạm vi (brand, category, product) cho khuyến mãi PRODUCT; loại khác thì xóa hết phạm vi. */
    private void saveScope(Promotion p, PromotionRequest r) {
        promotionBrandRepository.deleteByPromotion_PromotionId(p.getPromotionId());
        promotionCategoryRepository.deleteByPromotion_PromotionId(p.getPromotionId());
        promotionProductRepository.deleteByPromotion_PromotionId(p.getPromotionId());
        promotionBrandRepository.flush(); // xóa trước khi thêm lại cùng khóa chính kép

        if (p.getApplyType() != PromotionApplyType.PRODUCT) {
            return;
        }
        Set<String> brands = clean(r.getBrands());
        for (String brand : brands) {
            if (brand.length() > 100) {
                throw bad("Tên thương hiệu tối đa 100 ký tự");
            }
            promotionBrandRepository.save(PromotionBrand.builder()
                    .id(new PromotionBrandId(p.getPromotionId(), brand))
                    .promotion(p)
                    .build());
        }
        for (String categoryId : clean(r.getCategoryIds())) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Danh mục sản phẩm không tồn tại"));
            promotionCategoryRepository.save(PromotionCategory.builder()
                    .id(new PromotionCategoryId(p.getPromotionId(), categoryId))
                    .promotion(p)
                    .category(category)
                    .build());
        }
        for (String productId : clean(r.getProductIds())) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));
            promotionProductRepository.save(PromotionProduct.builder()
                    .id(new PromotionProductId(p.getPromotionId(), productId))
                    .promotion(p)
                    .product(product)
                    .build());
        }
    }

    private Set<String> clean(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    result.add(v.trim());
                }
            }
        }
        return result;
    }

    private PromotionResponse toResponse(Promotion p) {
        String id = p.getPromotionId();
        return PromotionResponse.builder()
                .promotionId(id)
                .name(p.getName())
                .description(p.getDescription())
                .applyType(p.getApplyType())
                .code(p.getCode())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minOrderValue(p.getMinOrderValue())
                .maxDiscountValue(p.getMaxDiscountValue())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.isStatus())
                .customerId(p.getCustomer() == null ? null : p.getCustomer().getUserId())
                .maxOrderUsage(p.getMaxOrderUsage())
                .brands(promotionBrandRepository.findByPromotion_PromotionId(id).stream()
                        .map(x -> x.getId().getBrand()).toList())
                .categoryIds(promotionCategoryRepository.findByPromotion_PromotionId(id).stream()
                        .map(x -> x.getId().getCategoryId()).toList())
                .productIds(promotionProductRepository.findByPromotion_PromotionId(id).stream()
                        .map(x -> x.getId().getProductId()).toList())
                .build();
    }

    private Promotion find(String promotionId) {
        return promotionRepository.findById(promotionId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khuyến mãi"));
    }

    private static int ceil1000(long x) {
        return (int) ((x + 999) / 1000 * 1000);
    }

    private static AppException bad(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }
}
