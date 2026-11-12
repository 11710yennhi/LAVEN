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
