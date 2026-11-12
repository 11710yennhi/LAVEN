package iuh.fit.laven.service;

import iuh.fit.laven.model.Product;
import iuh.fit.laven.model.ProductImage;
import iuh.fit.laven.repository.ProductImageRepository;
import iuh.fit.laven.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class ProductImageService {
    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(ProductImageRepository imageRepository, ProductRepository productRepository) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductImage> findByProduct(String productId, boolean includeInactiveProduct) {
        requireProduct(productId, includeInactiveProduct);
        return imageRepository.findByProductProductId(productId);
    }

    public ProductImage create(String productId, ProductImage image) {
        image.setProduct(requireProduct(productId, false));
        image.setProductImageId(nextImageId());
        return imageRepository.save(image);
    }

    public ProductImage update(String imageId, ProductImage request) {
        ProductImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> notFound("Ảnh sản phẩm"));
        image.setImageUrl(request.getImageUrl());
        return imageRepository.save(image);
    }

    private Product requireProduct(String id, boolean includeInactive) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> notFound("Sản phẩm"));
        if (!includeInactive && (!product.isStatus() || !product.getCategory().isStatus())) {
            throw notFound("Sản phẩm");
        }
        return product;
    }

    private String nextImageId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSS"));
        for (int number = 1; number <= 99; number++) {
            String id = "IMG" + timestamp + String.format("%02d", number);
            if (!imageRepository.existsById(id)) {
                return id;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể sinh mã ảnh, vui lòng thử lại");
    }

    private ResponseStatusException notFound(String entity) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " không tồn tại");
    }
}
