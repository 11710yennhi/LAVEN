package iuh.fit.laven.repository;

import iuh.fit.laven.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductProductId(String productId);

    List<ProductImage> findByProductProductIdOrderByProductImageIdAsc(String productId);

    // Chọn một ảnh cho mỗi sản phẩm, cùng thứ tự với bộ ảnh ở trang chi tiết.
    @Query("""
        SELECT img.product.productId AS productId, img.imageUrl AS imageUrl
        FROM ProductImage img
        WHERE img.productImageId IN (
            SELECT COALESCE(
                MAX(CASE WHEN candidate.productImageId = candidate.product.coverImageId THEN candidate.productImageId ELSE NULL END),
                MIN(candidate.productImageId)
            )
            FROM ProductImage candidate
            WHERE candidate.product.productId IN :productIds
            GROUP BY candidate.product.productId
        )
        """)
    List<CoverImage> findCovers(@Param("productIds") List<String> productIds);

    interface CoverImage {
        String getProductId();
        String getImageUrl();
    }

    @Query("""
        SELECT img FROM ProductImage img
        WHERE (:productId IS NULL OR img.product.productId = :productId)
          AND (:productName IS NULL OR LOWER(img.product.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
          AND (:productImageId IS NULL OR LOWER(img.productImageId) LIKE LOWER(CONCAT('%', :productImageId, '%')))
        ORDER BY img.productImageId ASC
        """)
    List<ProductImage> search(
        @Param("productId") String productId,
        @Param("productName") String productName,
        @Param("productImageId") String productImageId
    );
    boolean existsByImageUrl(String imageUrl);
}
