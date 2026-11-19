package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.ProductResponse;
import iuh.fit.laven.dto.response.StaffProductResponse;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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
        return productService.search(
            keyword, productId, productName, categoryId, brand, origin,
            material, size, colorId, minPrice, maxPrice,
            null, null, null, false
        ).stream().map(ProductResponse::new).toList();
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
        return productService.search(
            keyword, productId, productName, categoryId, brand, origin,
            material, size, colorId, minPrice, maxPrice,
            minOriginalPrice, maxOriginalPrice, status, true
        ).stream().map(StaffProductResponse::new).toList();
    }

    @GetMapping("/public/products/{productId}")
    public ProductResponse getPublicProduct(@PathVariable String productId) {
        return new ProductResponse(productService.findById(productId, false));
    }

    @GetMapping("/staff/products/{productId}")
    public StaffProductResponse getProduct(@PathVariable String productId) {
        return new StaffProductResponse(productService.findById(productId, true));
    }

    @PostMapping("/staff/products")
    public StaffProductResponse createProduct(@Valid @RequestBody Product product) {
        return new StaffProductResponse(productService.create(product));
    }

    @PutMapping("/staff/products/{productId}")
    public StaffProductResponse updateProduct(
        @PathVariable String productId,
        @Valid @RequestBody Product product
    ) {
        return new StaffProductResponse(productService.update(productId, product));
    }

    @DeleteMapping("/staff/products/{productId}")
    public StaffProductResponse deactivateProduct(
        @PathVariable String productId,
        @RequestParam(required = false) String reason
    ) {
        return new StaffProductResponse(productService.deactivate(productId, reason));
    }
}
