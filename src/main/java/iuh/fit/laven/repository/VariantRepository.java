package iuh.fit.laven.repository;

import iuh.fit.laven.model.Variant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, String> {
    List<Variant> findByProductProductId(String productId);

    @Query("""
        SELECT v FROM Variant v
        WHERE (:productId IS NULL OR v.product.productId = :productId)
          AND (:productName IS NULL OR LOWER(v.product.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
          AND (:keyword IS NULL
               OR LOWER(v.variantId) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.size) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:variantId IS NULL OR LOWER(v.variantId) LIKE LOWER(CONCAT('%', :variantId, '%')))
          AND (:size IS NULL OR LOWER(v.size) LIKE LOWER(CONCAT('%', :size, '%')))
          AND (:minWeight IS NULL OR v.weight >= :minWeight)
          AND (:maxWeight IS NULL OR v.weight <= :maxWeight)
          AND (:minCapacity IS NULL OR v.capacity >= :minCapacity)
          AND (:maxCapacity IS NULL OR v.capacity <= :maxCapacity)
          AND (:minLength IS NULL OR v.length >= :minLength)
          AND (:maxLength IS NULL OR v.length <= :maxLength)
          AND (:minWidth IS NULL OR v.width >= :minWidth)
          AND (:maxWidth IS NULL OR v.width <= :maxWidth)
          AND (:minHeight IS NULL OR v.height >= :minHeight)
          AND (:maxHeight IS NULL OR v.height <= :maxHeight)
        ORDER BY v.variantId ASC
        """)
    List<Variant> search(
        @Param("productId") String productId,
        @Param("productName") String productName,
        @Param("keyword") String keyword,
        @Param("variantId") String variantId,
        @Param("size") String size,
        @Param("minWeight") Double minWeight,
        @Param("maxWeight") Double maxWeight,
        @Param("minCapacity") Double minCapacity,
        @Param("maxCapacity") Double maxCapacity,
        @Param("minLength") Double minLength,
        @Param("maxLength") Double maxLength,
        @Param("minWidth") Double minWidth,
        @Param("maxWidth") Double maxWidth,
        @Param("minHeight") Double minHeight,
        @Param("maxHeight") Double maxHeight
    );

    Optional<Variant> findByVariantId(String variantId);
}
