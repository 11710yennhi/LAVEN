package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.dto.response.StaffVariantResponse;
import iuh.fit.laven.dto.response.VariantResponse;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class VariantController {
    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping("/public/variants")
    public List<VariantResponse> searchPublicVariants(
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
        ).stream().map(VariantResponse::new).toList();
    }

    @GetMapping("/staff/variants")
    public List<StaffVariantResponse> searchVariants(
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
        ).stream().map(StaffVariantResponse::new).toList();
    }

    @GetMapping("/public/products/{productId}/variants")
    public List<VariantResponse> getPublicVariants(@PathVariable String productId) {
        return variantService.findByProduct(productId, false).stream().map(VariantResponse::new).toList();
    }

    @GetMapping("/staff/products/{productId}/variants")
    public List<StaffVariantResponse> getVariants(@PathVariable String productId) {
        return variantService.findByProduct(productId, true).stream().map(StaffVariantResponse::new).toList();
    }

    @PostMapping("/staff/products/{productId}/variants")
    public StaffVariantResponse addVariant(
        @PathVariable String productId,
        @Valid @RequestBody Variant variant
    ) {
        return new StaffVariantResponse(variantService.create(productId, variant));
    }

    @PutMapping("/staff/variants/{variantId}")
    public StaffVariantResponse updateVariant(
        @PathVariable String variantId,
        @Valid @RequestBody Variant variant
    ) {
        return new StaffVariantResponse(variantService.update(variantId, variant));
    }
}
