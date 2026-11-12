package iuh.fit.laven.service;

import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.Variant;
import iuh.fit.laven.repository.ProductRepository;
import iuh.fit.laven.repository.VariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class VariantService {
    private final VariantRepository variantRepository;
    private final ProductRepository productRepository;

    public VariantService(VariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Variant> findByProduct(String productId, boolean includeInactiveProduct) {
        requireProduct(productId, includeInactiveProduct);
        return variantRepository.findByProductProductId(productId);
    }

    public Variant create(String productId, Variant variant) {
        variant.setProduct(requireProduct(productId, false));
        variant.setVariantId(nextVariantId());
        return variantRepository.save(variant);
    }

    public Variant update(String id, Variant request) {
        Variant variant = variantRepository.findById(id)
            .orElseThrow(() -> notFound("Biến thể"));
        variant.setSize(request.getSize());
        variant.setWeight(request.getWeight());
        variant.setCapacity(request.getCapacity());
        variant.setLength(request.getLength());
        variant.setWidth(request.getWidth());
        variant.setHeight(request.getHeight());
        return variantRepository.save(variant);
    }

    private Product requireProduct(String id, boolean includeInactive) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> notFound("Sản phẩm"));
        if (!includeInactive && (!product.isStatus() || !product.getCategory().isStatus())) {
            throw notFound("Sản phẩm");
        }
        return product;
    }

    private String nextVariantId() {
        LocalDateTime now = LocalDateTime.now();
        String second = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int hundredth = now.getNano() / 10_000_000;
        for (int offset = 0; offset < 100; offset++) {
            String id = "VAR" + second + String.format("%02d", (hundredth + offset) % 100);
            if (!variantRepository.existsById(id)) {
                return id;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể sinh mã biến thể, vui lòng thử lại");
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " không tồn tại");
    }
}
