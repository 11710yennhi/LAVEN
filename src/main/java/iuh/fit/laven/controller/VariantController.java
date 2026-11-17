package iuh.fit.laven.controller;

import iuh.fit.laven.model.Variant;
import iuh.fit.laven.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VariantController {
    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/public/variants")
    public List<Variant> searchPublicVariants(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String variantId,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) Double minWeight,
        @RequestParam(required = false) Double maxWeight,
        @RequestParam(required = false) Double minCapacity,
        @RequestParam(required = false) Double maxCapacity,
        @RequestParam(required = false) Double minLength,
        @RequestParam(required = false) Double maxLength,
        @RequestParam(required = false) Double minWidth,
        @RequestParam(required = false) Double maxWidth,
        @RequestParam(required = false) Double minHeight,
        @RequestParam(required = false) Double maxHeight
    ) {
        return variantService.search(
            productId, productName, keyword, variantId, size,
            minWeight, maxWeight, minCapacity, maxCapacity,
            minLength, maxLength, minWidth, maxWidth, minHeight, maxHeight
        );
    }

    @GetMapping("/staff/variants")
    public List<Variant> searchVariants(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String variantId,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) Double minWeight,
        @RequestParam(required = false) Double maxWeight,
        @RequestParam(required = false) Double minCapacity,
        @RequestParam(required = false) Double maxCapacity,
        @RequestParam(required = false) Double minLength,
        @RequestParam(required = false) Double maxLength,
        @RequestParam(required = false) Double minWidth,
        @RequestParam(required = false) Double maxWidth,
        @RequestParam(required = false) Double minHeight,
        @RequestParam(required = false) Double maxHeight
    ) {
        return variantService.search(
            productId, productName, keyword, variantId, size,
            minWeight, maxWeight, minCapacity, maxCapacity,
            minLength, maxLength, minWidth, maxWidth, minHeight, maxHeight
        );
    }

    @GetMapping("/public/products/{productId}/variants")
    public List<Variant> getPublicVariants(@PathVariable String productId) {
        return variantService.findByProduct(productId, false);
    }

    @GetMapping("/staff/products/{productId}/variants")
    public List<Variant> getVariants(@PathVariable String productId) {
        return variantService.findByProduct(productId, true);
    }

    @PostMapping("/staff/products/{productId}/variants")
    public Variant addVariant(
        @PathVariable String productId,
        @Valid @RequestBody Variant variant
    ) {
        return variantService.create(productId, variant);
    }

    @PutMapping("/staff/variants/{variantId}")
    public Variant updateVariant(
        @PathVariable String variantId,
        @Valid @RequestBody Variant variant
    ) {
        return variantService.update(variantId, variant);
    }
}
