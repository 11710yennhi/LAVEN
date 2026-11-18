package iuh.fit.laven.controller;

import iuh.fit.laven.dto.request.ProductImageRequest;
import iuh.fit.laven.dto.response.MessageResponse;
import iuh.fit.laven.dto.response.ProductImageUploadSignatureResponse;
import iuh.fit.laven.model.ProductImage;
import iuh.fit.laven.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductImageController {
    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    /**
     * 1. Bước 1 (Direct Upload): Lấy chữ ký số Cloudinary trước khi gửi file từ trình duyệt
     */
    @PostMapping("/staff/products/{productId}/images/upload-signature")
    public ProductImageUploadSignatureResponse getUploadSignature(@PathVariable String productId) {
        return productImageService.getUploadSignature(productId);
    }

    /**
     * 2. Thêm ảnh (Add): Nhận URL từ React sau khi tải lên Cloudinary thành công
     */
    @PostMapping("/staff/products/{productId}/images")
    public ProductImage addImage(
        @PathVariable String productId,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return productImageService.addImage(productId, request.getImageUrl());
    }

    /**
     * 3. Cập nhật ảnh (Update): Thay URL ảnh mới (đồng thời tự xóa ảnh cũ trên Cloudinary)
     */
    @PutMapping("/staff/images/{imageId}")
    public ProductImage updateImage(
        @PathVariable String imageId,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return productImageService.updateImage(imageId, request.getImageUrl());
    }

    /**
     * 4. Xóa ảnh (Delete): Xóa bản ghi trong DB và xóa file ảnh trên Cloudinary
     */
    @DeleteMapping("/staff/images/{imageId}")
    public MessageResponse deleteImage(@PathVariable String imageId) {
        productImageService.deleteImage(imageId);
        return new MessageResponse("Xóa ảnh sản phẩm thành công");
    }

    // ===== Các API tìm kiếm & đọc ảnh =====

    @GetMapping("/public/images")
    public List<ProductImage> searchPublicImages(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String productImageId
    ) {
        return productImageService.search(productId, productName, productImageId);
    }

    @GetMapping("/staff/images")
    public List<ProductImage> searchImages(
        @RequestParam(required = false) String productId,
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String productImageId
    ) {
        return productImageService.search(productId, productName, productImageId);
    }

    @GetMapping("/public/products/{productId}/images")
    public List<ProductImage> getPublicImages(@PathVariable String productId) {
        return productImageService.findByProduct(productId, false);
    }

    @GetMapping("/staff/products/{productId}/images")
    public List<ProductImage> getImages(@PathVariable String productId) {
        return productImageService.findByProduct(productId, true);
    }
}
