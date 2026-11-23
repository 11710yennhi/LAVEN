package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.StaffProductResponse;
import iuh.fit.laven.model.Product;
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

    public ProductController(ProductService productService, ProductImageService productImageService) {
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
    public ProductResponse getPublicProduct(@PathVariable String productId) {
        Product product = productService.findById(productId, false);
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
