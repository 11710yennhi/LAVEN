package iuh.fit.laven.repository;

import iuh.fit.laven.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductProductId(String productId);

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
}
