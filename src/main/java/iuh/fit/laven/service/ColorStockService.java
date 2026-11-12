package iuh.fit.laven.service;

import iuh.fit.laven.model.Color;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.ColorStockId;
import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.repository.ColorRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.VariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ColorStockService {
    private final ColorStockRepository colorStockRepository;
    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final ColorRepository colorRepository;

    public ColorStockService(ColorStockRepository colorStockRepository,
                             ProductRepository productRepository,
                             VariantRepository variantRepository,
                             ColorRepository colorRepository) {
        this.colorStockRepository = colorStockRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.colorRepository = colorRepository;
    }

    @Transactional(readOnly = true)
    public List<ColorStock> findByProduct(String productId, boolean includeInactiveProduct) {
        requireProduct(productId, includeInactiveProduct);
        return colorStockRepository.findByVariantProductProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<ColorStock> findLowStock(int threshold) {
        if (threshold < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngưỡng tồn kho phải >= 0");
        }
        return colorStockRepository.findAll().stream()
            .filter(stock -> stock.getStock() <= threshold)
            .filter(stock -> stock.getVariant().getProduct().isStatus())
            .filter(stock -> stock.getVariant().getProduct().getCategory().isStatus())
            .toList();
    }

    @Transactional(readOnly = true)
    public ColorStock findById(String variantId, String colorId) {
        return colorStockRepository.findById(new ColorStockId(variantId, colorId))
            .orElseThrow(() -> notFound("Tồn kho biến thể-màu"));
    }

    public ColorStock setStock(String variantId, String colorId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho phải >= 0");
        }

        ColorStockId id = new ColorStockId(variantId, colorId);
        ColorStock stock = colorStockRepository.findLocked(id)
            .orElseGet(() -> createStock(variantId, colorId, id));
        stock.setStock(quantity);
        return colorStockRepository.save(stock);
    }

    public ColorStock adjustStock(String variantId, String colorId, int delta) {
        ColorStock stock = colorStockRepository.findLocked(new ColorStockId(variantId, colorId))
            .orElseThrow(() -> notFound("Tồn kho biến thể-màu"));
        long updatedStock = (long) stock.getStock() + delta;
        if (updatedStock < 0 || updatedStock > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Điều chỉnh làm tồn kho âm hoặc vượt giới hạn");
        }
        stock.setStock((int) updatedStock);
        return colorStockRepository.save(stock);
    }

    private ColorStock createStock(String variantId, String colorId, ColorStockId id) {
        Variant variant = variantRepository.findById(variantId)
            .orElseThrow(() -> notFound("Biến thể"));
        Color color = colorRepository.findById(colorId)
            .orElseThrow(() -> notFound("Màu sắc"));
        requireProduct(variant.getProduct().getProductId(), false);

        ColorStock stock = new ColorStock();
        stock.setId(id);
        stock.setVariant(variant);
        stock.setColor(color);
        return stock;
    }

    private Product requireProduct(String id, boolean includeInactive) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> notFound("Sản phẩm"));
        if (!includeInactive && (!product.isStatus() || !product.getCategory().isStatus())) {
            throw notFound("Sản phẩm");
        }
        return product;
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " không tồn tại");
    }
}
