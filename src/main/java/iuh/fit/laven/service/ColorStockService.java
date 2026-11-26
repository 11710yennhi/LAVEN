package iuh.fit.laven.service;

import iuh.fit.laven.model.Color;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.id.ColorStockId;
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
    private final org.springframework.context.ApplicationEventPublisher events;

    public ColorStockService(ColorStockRepository colorStockRepository,
                             ProductRepository productRepository,
                             VariantRepository variantRepository,
                             ColorRepository colorRepository,
                             org.springframework.context.ApplicationEventPublisher events) {
        this.colorStockRepository = colorStockRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.colorRepository = colorRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ColorStock> findByProduct(String productId, boolean includeInactiveProduct) {
        requireProduct(productId, includeInactiveProduct);
        return colorStockRepository.findByVariantProductProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<ColorStock> search(String productId, String productName,
                                   String variantId, String size,
                                   String colorId, String colorName,
                                   Integer minStock, Integer maxStock,
                                   boolean includeInactive) {
        if ((minStock != null && minStock < 0)
            || (maxStock != null && maxStock < 0)
            || (minStock != null && maxStock != null && minStock > maxStock)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng tồn kho không hợp lệ");
        }

        return colorStockRepository.search(
            blank(productId), blank(productName),
            blank(variantId), blank(size),
            blank(colorId), blank(colorName),
            minStock, maxStock,
            includeInactive
        );
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

    public ColorStock createStock(String variantId, String colorId, Integer quantity) {
        return createStock(variantId, colorId, quantity, null);
    }

    public ColorStock createStock(String variantId, String colorId, Integer quantity, String imageUrl) {
        if (quantity == null || quantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tồn kho phải >= 0");
        }

        ColorStockId id = new ColorStockId(variantId, colorId);
        // Chỉ tạo dòng tồn kho ban đầu; nhập thêm hàng dùng adjustStock(delta).
        if (colorStockRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tồn kho đã tồn tại; hãy dùng điều chỉnh số lượng");
        }
        ColorStock stock = buildStock(variantId, colorId, id);
        stock.setStock(quantity);
        stock.setImageUrl(iuh.fit.laven.util.ValidationUtil.optionalImageUrl(imageUrl));
        return colorStockRepository.save(stock);
    }

    public ColorStock updateImage(String variantId, String colorId, String imageUrl) {
        ColorStock stock = colorStockRepository.findLocked(new ColorStockId(variantId, colorId))
            .orElseThrow(() -> notFound("Tồn kho biến thể-màu"));
        String previousUrl = stock.getImageUrl();
        String newUrl = iuh.fit.laven.util.ValidationUtil.optionalImageUrl(imageUrl);
        stock.setImageUrl(newUrl);
        ColorStock saved = colorStockRepository.save(stock);
        if (previousUrl != null && !previousUrl.equals(newUrl)) {
            events.publishEvent(new ProductImageCleanup.RemovedImage(previousUrl));
        }
        return saved;
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

    private ColorStock buildStock(String variantId, String colorId, ColorStockId id) {
        Variant variant = variantRepository.findById(variantId)
            .orElseThrow(() -> notFound("Biến thể"));
        Color color = colorRepository.findById(colorId)
            .orElseThrow(() -> notFound("Màu sắc"));
        if (!variant.getProduct().getCategory().isStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục đã ngừng kinh doanh");
        }

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

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " không tồn tại");
    }
}
