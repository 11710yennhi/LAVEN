package iuh.fit.laven.controller;

import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.service.ColorStockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ColorStockController {
    private final ColorStockService colorStockService;

    public ColorStockController(ColorStockService colorStockService) {
        this.colorStockService = colorStockService;
    }

    @GetMapping("/public/products/{productId}/stocks")
    public List<ColorStock> getPublicStocks(@PathVariable String productId) {
        return colorStockService.findByProduct(productId, false);
    }

    @GetMapping("/staff/stocks")
    public List<ColorStock> getStocks(@RequestParam String productId) {
        return colorStockService.findByProduct(productId, true);
    }

    @GetMapping("/staff/stocks/low-stock")
    public List<ColorStock> getLowStock(
        @RequestParam(defaultValue = "5") int threshold
    ) {
        return colorStockService.findLowStock(threshold);
    }

    @GetMapping("/staff/stocks/{variantId}/{colorId}")
    public ColorStock getStock(
        @PathVariable String variantId,
        @PathVariable String colorId
    ) {
        return colorStockService.findById(variantId, colorId);
    }

    @PutMapping("/staff/stocks/{variantId}/{colorId}")
    public ColorStock setStock(
        @PathVariable String variantId,
        @PathVariable String colorId,
        @RequestParam Integer quantity
    ) {
        return colorStockService.setStock(variantId, colorId, quantity);
    }

    @PostMapping("/staff/stocks/{variantId}/{colorId}/adjustment")
    public ColorStock adjustStock(
        @PathVariable String variantId,
        @PathVariable String colorId,
        @RequestParam int delta
    ) {
        return colorStockService.adjustStock(variantId, colorId, delta);
    }
}
