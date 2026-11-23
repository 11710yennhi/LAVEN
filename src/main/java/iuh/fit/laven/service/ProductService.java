package iuh.fit.laven.service;

import iuh.fit.laven.dto.response.SalePriceRange;
import iuh.fit.laven.model.Category;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.repository.CategoryRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.VariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VariantRepository variantRepository;
    private final ColorStockRepository colorStockRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          VariantRepository variantRepository,
                          ColorStockRepository colorStockRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
        this.colorStockRepository = colorStockRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> search(String keyword, String categoryId, String brand,
                                String material, String size, String colorId,
                                Integer minPrice, Integer maxPrice, boolean includeInactive) {
        return search(keyword, null, null, categoryId, brand, null, material, size, colorId,
                      minPrice, maxPrice, null, null, null, includeInactive);
    }

    @Transactional(readOnly = true)
    public List<Product> search(String keyword, String productId, String productName,
                                String categoryId, String brand, String origin,
                                String material, String size, String colorId,
                                Integer minPrice, Integer maxPrice,
                                Integer minOriginalPrice, Integer maxOriginalPrice,
                                Boolean status, boolean includeInactive) {
        if ((minPrice != null && minPrice < 0)
            || (maxPrice != null && maxPrice < 0)
            || (minPrice != null && maxPrice != null && minPrice > maxPrice)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng giá bán không hợp lệ");
        }
        if ((minOriginalPrice != null && minOriginalPrice < 0)
            || (maxOriginalPrice != null && maxOriginalPrice < 0)
            || (minOriginalPrice != null && maxOriginalPrice != null && minOriginalPrice > maxOriginalPrice)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng giá gốc không hợp lệ");
        }

        List<Product> products = productRepository.search(
                blank(keyword), blank(productId), blank(productName),
                blank(categoryId), blank(brand), blank(origin),
                blank(material), status, includeInactive)
            .stream()
            .filter(product -> matchesSize(product, size))
            .filter(product -> matchesColor(product, colorId))
            .toList();
        if (minPrice == null && maxPrice == null && minOriginalPrice == null && maxOriginalPrice == null) {
            return products;
        }

        // Giá nằm ở biến thể: sản phẩm khớp nếu có ít nhất 1 size nằm trong cả 2 khoảng giá
        Map<String, List<Variant>> variants = variantsByProduct(products);
        return products.stream()
            .filter(product -> variants.getOrDefault(product.getProductId(), List.of()).stream()
                .anyMatch(variant -> inRange(product.calcSalePrice(variant.getOriginalPrice()), minPrice, maxPrice)
                    && inRange(variant.getOriginalPrice(), minOriginalPrice, maxOriginalPrice)))
            .toList();
    }

    /** Khoảng giá bán theo các size của từng sản phẩm (productId → min/max). */
    @Transactional(readOnly = true)
    public Map<String, SalePriceRange> findSalePriceRanges(Collection<Product> products) {
        Map<String, List<Variant>> variants = variantsByProduct(products);
        return products.stream().collect(Collectors.toMap(Product::getProductId, product -> {
            IntSummaryStatistics stats = variants.getOrDefault(product.getProductId(), List.of()).stream()
                .map(variant -> product.calcSalePrice(variant.getOriginalPrice()))
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .summaryStatistics();
            return stats.getCount() == 0
                ? new SalePriceRange(null, null)
                : new SalePriceRange(stats.getMin(), stats.getMax());
        }, (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public SalePriceRange findSalePriceRange(Product product) {
        return findSalePriceRanges(List.of(product)).get(product.getProductId());
    }

    private Map<String, List<Variant>> variantsByProduct(Collection<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        return variantRepository.findByProductProductIdIn(products.stream().map(Product::getProductId).toList())
            .stream()
            .collect(Collectors.groupingBy(variant -> variant.getProduct().getProductId()));
    }

    private boolean inRange(Integer value, Integer min, Integer max) {
        return value != null && (min == null || value >= min) && (max == null || value <= max);
    }

    @Transactional(readOnly = true)
    public Product findById(String id, boolean includeInactive) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> notFound("Sản phẩm"));
        if (!includeInactive && (!product.isStatus() || !product.getCategory().isStatus())) {
            throw notFound("Sản phẩm");
        }
        return product;
    }

    public Product create(Product product) {
        if (product.getProductId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã sản phẩm do hệ thống sinh");
        }
        validate(product);
        product.setCategory(findActiveCategory(product));
        product.setProductId(nextProductId());
        product.setStatus(true);
        product.setStopSellingReason(null);
        return productRepository.save(product);
    }

    public Product update(String id, Product request) {
        Product product = findById(id, true);
        validate(request);
        product.setCategory(findActiveCategory(request));
        product.setProductName(request.getProductName());
        product.setBrand(request.getBrand());
        product.setOrigin(request.getOrigin());
        product.setMaterial(request.getMaterial());
        product.setDescription(request.getDescription());
        product.setWaterResistance(request.getWaterResistance());
        product.setWheel(request.getWheel());
        product.setZipper(request.getZipper());
        product.setWarranty(request.getWarranty());
        product.setMaxQuantityPerOrder(request.getMaxQuantityPerOrder());
        return productRepository.save(product);
    }

    public Product deactivate(String id, String reason) {
        Product product = findById(id, true);
        product.setStatus(false);
        product.setStopSellingReason(
            reason == null || reason.isBlank() ? "Ngừng bán theo yêu cầu quản trị" : reason.trim());
        return productRepository.save(product);
    }

    private Category findActiveCategory(Product product) {
        if (product.getCategory() == null || product.getCategory().getCategoryId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục sản phẩm không được rỗng");
        }
        Category category = categoryRepository.findById(product.getCategory().getCategoryId())
            .orElseThrow(() -> notFound("Danh mục"));
        if (!category.isStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục đã ngừng kinh doanh");
        }
        return category;
    }

    private void validate(Product product) {
        if (product.getMaxQuantityPerOrder() == null || product.getMaxQuantityPerOrder() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tối đa phải lớn hơn 0");
        }
    }

    private boolean matchesSize(Product product, String size) {
        return blank(size) == null || variantRepository.findByProductProductId(product.getProductId())
            .stream().anyMatch(variant -> variant.getSize().equalsIgnoreCase(size.trim()));
    }

    private boolean matchesColor(Product product, String colorId) {
        return blank(colorId) == null
            || colorStockRepository.findByVariantProductProductId(product.getProductId())
                .stream().anyMatch(stock -> stock.getColor().getColorId().equalsIgnoreCase(colorId.trim()));
    }

    private String nextProductId() {
        LocalDateTime now = LocalDateTime.now();
        String second = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int hundredth = now.getNano() / 10_000_000;
        for (int offset = 0; offset < 100; offset++) {
            String id = "PRO" + second + String.format("%02d", (hundredth + offset) % 100);
            if (!productRepository.existsById(id)) {
                return id;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể sinh mã sản phẩm, vui lòng thử lại");
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " không tồn tại");
    }
}
