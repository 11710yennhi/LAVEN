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

@Service
@Transactional
public class ProductImageService {
    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageCloudinaryService cloudinaryService;

    public ProductImageService(ProductImageRepository imageRepository,
                               ProductRepository productRepository,
                               ProductImageCloudinaryService cloudinaryService) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public List<ProductImage> findByProduct(String productId, boolean includeInactiveProduct) {
        requireProduct(productId, includeInactiveProduct);
        return imageRepository.findByProductProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductImage> search(String productId, String productName, String productImageId) {
        return imageRepository.search(blank(productId), blank(productName), blank(productImageId));
    }

    /**
     * 1. Lấy chữ ký số để React gửi file thẳng lên Cloudinary
     */
    public ProductImageUploadSignatureResponse getUploadSignature(String productId) {
        requireProduct(productId, false);
        return cloudinaryService.signUpload(productId);
    }

    /**
     * 2. Thêm ảnh mới (Add): Nhận URL từ React sau khi tải lên Cloudinary
     */
    public ProductImage addImage(String productId, String imageUrl) {
        Product product = requireProduct(productId, false);
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
        cloudinaryService.deleteByUrl(oldImageUrl);
        return saved;
    }

    /**
     * 4. Xóa ảnh (Delete): Xóa bản ghi trong DB và xóa luôn file trên Cloudinary
     */
    public void deleteImage(String imageId) {
        ProductImage image = imageRepository.findById(imageId)
            .orElseThrow(() -> notFound("Ảnh sản phẩm"));

        cloudinaryService.deleteByUrl(image.getImageUrl());
        imageRepository.delete(image);
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
