package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.StaffProductResponse;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.UserEvent.Type;
import iuh.fit.laven.security.CustomUserDetails;
import iuh.fit.laven.service.UserEventService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import iuh.fit.laven.service.ProductImageService;
import iuh.fit.laven.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {
    private final ProductService productService;
    private final ProductImageService productImageService;

    private final UserEventService events;

    public ProductController(ProductService productService, ProductImageService productImageService, UserEventService events) {
        this.events = events;
        this.productService = productService;
        this.productImageService = productImageService;
    }

    @GetMapping("/public/products")
    public List<ProductResponse> searchPublicProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String origin,
        @RequestParam(required = false) String material,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice
    ) {
        List<Product> products = productService.search(
            keyword, productId, productName, categoryId, brand, origin,
            material, size, colorId, minPrice, maxPrice,
            null, null, null, false
        );
        var coverUrls = productImageService.findCoverUrls(products.stream().map(Product::getProductId).toList());
        var priceRanges = productService.findSalePriceRanges(products);
        return products.stream()
            .map(product -> new ProductResponse(product, priceRanges.get(product.getProductId()),
                coverUrls.get(product.getProductId())))
            .toList();
    }

    public record FeaturedProduct(ProductResponse product, String size, long score) {}

    @GetMapping("/public/products/featured")
    public List<FeaturedProduct> featured(@RequestParam(required = false) String categoryId) {
        var ranked = events.featured(categoryId);
        var ids = ranked.stream().map(UserEventService.RankedProduct::productId).toList();
        var covers = productImageService.findCoverUrls(ids);
        var products = ids.stream().map(id -> productService.findById(id, false)).toList();
        var ranges = productService.findSalePriceRanges(products);
        return java.util.stream.IntStream.range(0, ranked.size()).mapToObj(index -> {
            var row = ranked.get(index);
            return new FeaturedProduct(new ProductResponse(products.get(index), ranges.get(row.productId()),
                covers.get(row.productId())), row.size(), row.score());
        }).toList();
    }

    @GetMapping("/staff/products")
    public List<StaffProductResponse> searchProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) String brand,
        @RequestParam(required = false) String origin,
        @RequestParam(required = false) String material,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(required = false) Integer minOriginalPrice,
        @RequestParam(required = false) Integer maxOriginalPrice,
        @RequestParam(required = false) Boolean status
    ) {
        // minOriginalPrice/maxOriginalPrice: lọc theo giá nhập của biến thể (có ít nhất 1 size nằm trong khoảng)
        List<Product> products = productService.search(
            keyword, productId, productName, categoryId, brand, origin,
            material, size, colorId, minPrice, maxPrice,
            minOriginalPrice, maxOriginalPrice, status, true
        );
        var priceRanges = productService.findSalePriceRanges(products);
        return products.stream()
            .map(product -> new StaffProductResponse(product, priceRanges.get(product.getProductId())))
            .toList();
    }

    @GetMapping("/public/products/{productId}")
    public ProductResponse getPublicProduct(@PathVariable String productId, @AuthenticationPrincipal CustomUserDetails principal) {
        Product product = productService.findById(productId, false);
        if (principal != null && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            events.record(principal.getUserId(), productId, Type.VIEW);
        }
        return new ProductResponse(product, productService.findSalePriceRange(product));
    }

    @GetMapping("/staff/products/{productId}")
    public StaffProductResponse getProduct(@PathVariable String productId) {
        return toStaffResponse(productService.findById(productId, true));
    }

    @PostMapping("/staff/products")
    public StaffProductResponse createProduct(@Valid @RequestBody Product product) {
        return toStaffResponse(productService.create(product));
    }

    @PutMapping("/staff/products/{productId}")
    public StaffProductResponse updateProduct(
        @PathVariable String productId,
        @Valid @RequestBody Product product
    ) {
        return toStaffResponse(productService.update(productId, product));
    }

    @DeleteMapping("/staff/products/{productId}")
    public StaffProductResponse deactivateProduct(
        @PathVariable String productId,
        @RequestParam(required = false) String reason
    ) {
        return toStaffResponse(productService.deactivate(productId, reason));
    }

    private StaffProductResponse toStaffResponse(Product product) {
        return new StaffProductResponse(product, productService.findSalePriceRange(product));
    }
}
