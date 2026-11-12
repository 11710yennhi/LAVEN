package iuh.fit.laven.controller;

import iuh.fit.laven.model.ProductImage;
import iuh.fit.laven.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductImageController {
    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @GetMapping("/public/products/{productId}/images")
    public List<ProductImage> getPublicImages(@PathVariable String productId) {
        return productImageService.findByProduct(productId, false);
    }

    @GetMapping("/staff/products/{productId}/images")
    public List<ProductImage> getImages(@PathVariable String productId) {
        return productImageService.findByProduct(productId, true);
    }

    @PostMapping("/staff/products/{productId}/images")
    public ProductImage addImage(
        @PathVariable String productId,
        @Valid @RequestBody ProductImage image
    ) {
        return productImageService.create(productId, image);
    }

    @PutMapping("/staff/images/{imageId}")
    public ProductImage updateImage(
        @PathVariable String imageId,
        @Valid @RequestBody ProductImage image
    ) {
        return productImageService.update(imageId, image);
    }
}
