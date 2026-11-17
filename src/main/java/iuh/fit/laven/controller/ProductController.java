package iuh.fit.laven.controller;

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
    public List<Product> searchPublicProducts(
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
        @RequestParam(required = false) Integer maxOriginalPrice
    ) {
        return productService.search(
            keyword, productId, productName, categoryId, brand, origin,
            material, size, colorId, minPrice, maxPrice,
            minOriginalPrice, maxOriginalPrice, null, false
        );
    }

    @GetMapping("/staff/products")
    public List<Product> searchProducts(
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
        );
    }

    @GetMapping("/public/products/{productId}")
    public Product getPublicProduct(@PathVariable String productId) {
        return productService.findById(productId, false);
    }

    @GetMapping("/staff/products/{productId}")
    public Product getProduct(@PathVariable String productId) {
        return productService.findById(productId, true);
    }

    @PostMapping("/staff/products")
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/staff/products/{productId}")
    public Product updateProduct(
        @PathVariable String productId,
        @Valid @RequestBody Product product
    ) {
        return productService.update(productId, product);
    }

    @DeleteMapping("/staff/products/{productId}")
    public Product deactivateProduct(
        @PathVariable String productId,
        @RequestParam(required = false) String reason
    ) {
        return productService.deactivate(productId, reason);
    }
}
