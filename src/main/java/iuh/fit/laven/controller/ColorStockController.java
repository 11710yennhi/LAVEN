package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.service.ColorStockService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ColorStockController {
    private final ColorStockService colorStockService;

    public ColorStockController(ColorStockService colorStockService) {
        this.colorStockService = colorStockService;
    }

    @GetMapping("/public/stocks")
    public List<ColorStock> searchPublicStocks(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String variantId,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) String colorName,
        @RequestParam(required = false) Integer minStock,
        @RequestParam(required = false) Integer maxStock
    ) {
        return colorStockService.search(
            productId, productName, variantId, size,
            colorId, colorName, minStock, maxStock, false
        );
    }

    @GetMapping("/public/products/{productId}/stocks")
    public List<ColorStock> getPublicStocks(@PathVariable String productId) {
        return colorStockService.findByProduct(productId, false);
    }

    @GetMapping("/staff/stocks")
    public List<ColorStock> getStocks(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String variantId,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) String colorName,
        @RequestParam(required = false) Integer minStock,
        @RequestParam(required = false) Integer maxStock
    ) {
        return colorStockService.search(
            productId, productName, variantId, size,
            colorId, colorName, minStock, maxStock, true
        );
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

    // Tạo tồn kho lần đầu. Nhập thêm hàng dùng /adjustment với delta dương.
    @PostMapping("/staff/stocks/{variantId}/{colorId}")
    public ColorStock createStock(
        @PathVariable String variantId,
        @PathVariable String colorId,
        @RequestParam Integer quantity
    ) {
        return colorStockService.createStock(variantId, colorId, quantity);
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
