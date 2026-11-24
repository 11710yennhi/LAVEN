package iuh.fit.laven.service;

import iuh.fit.laven.dto.response.ProductImageUploadSignatureResponse;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductImageService {
    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageCloudinaryService cloudinaryService;
    private final org.springframework.context.ApplicationEventPublisher events;

    public ProductImageService(ProductImageRepository imageRepository,
                               ProductRepository productRepository,
                               ProductImageCloudinaryService cloudinaryService,
                               org.springframework.context.ApplicationEventPublisher events) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.cloudinaryService = cloudinaryService;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ProductImage> findByProduct(String productId, boolean includeInactiveProduct) {
        Product product = requireProduct(productId, includeInactiveProduct);
        return imageRepository.findByProductProductIdOrderByProductImageIdAsc(productId).stream()
            .sorted(java.util.Comparator.comparing(image -> !image.getProductImageId().equals(product.getCoverImageId())))
            .toList();
    }

    public void setCover(String productId, String imageId) {
        Product product = requireProduct(productId, true);
        ProductImage image = imageRepository.findById(imageId)
            .filter(candidate -> candidate.getProduct().getProductId().equals(productId))
            .orElseThrow(() -> notFound("Ảnh sản phẩm"));
        product.setCoverImageId(image.getProductImageId());
    }

    @Transactional(readOnly = true)
    public Map<String, String> findCoverUrls(List<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findCovers(productIds).stream()
            .collect(Collectors.toMap(ProductImageRepository.CoverImage::getProductId,
                                      ProductImageRepository.CoverImage::getImageUrl));
    }

    @Transactional(readOnly = true)
    public List<ProductImage> search(String productId, String productName, String productImageId) {
        return imageRepository.search(blank(productId), blank(productName), blank(productImageId));
    }

    /**
     * 1. Lấy chữ ký số để React gửi file thẳng lên Cloudinary
     */
    public ProductImageUploadSignatureResponse getUploadSignature(String productId) {
        Product product = requireProduct(productId, true);
        return cloudinaryService.signUpload(productId, product.getCategory().getCategoryName());
    }

    /**
     * 2. Thêm ảnh mới (Add): Nhận URL từ React sau khi tải lên Cloudinary
     */
    public ProductImage addImage(String productId, String imageUrl) {
        Product product = requireProduct(productId, true);
        String validUrl = cloudinaryService.validateImageUrl(imageUrl, productId);

        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl(validUrl);
        image.setProductImageId(nextImageId());
        return imageRepository.save(image);
    }

    /**
     * 3. Cập nhật ảnh (Update): Thay URL ảnh mới và tự động dọn ảnh cũ trên Cloudinary
     */
    public ProductImage updateImage(String imageId, String newImageUrl) {
        ProductImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> notFound("Ảnh sản phẩm"));

        String validUrl = cloudinaryService.validateImageUrl(newImageUrl, image.getProduct().getProductId());
        String oldImageUrl = image.getImageUrl();

        image.setImageUrl(validUrl);
        ProductImage saved = imageRepository.save(image);

        // Dọn ảnh cũ trên Cloudinary
        if (oldImageUrl != null && !oldImageUrl.equals(validUrl)) {
            events.publishEvent(new ProductImageCleanup.RemovedImage(oldImageUrl));
        }
        return saved;
    }

    /**
     * 4. Xóa ảnh (Delete): Xóa bản ghi trong DB và xóa luôn file trên Cloudinary
     */
    public void deleteImage(String imageId) {
        ProductImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> notFound("Ảnh sản phẩm"));


        Product product = image.getProduct();
        if (imageId.equals(product.getCoverImageId())) {
            product.setCoverImageId(null);
        }
        imageRepository.delete(image);
        events.publishEvent(new ProductImageCleanup.RemovedImage(image.getImageUrl()));
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
