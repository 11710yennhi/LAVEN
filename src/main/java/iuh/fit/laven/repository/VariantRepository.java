package iuh.fit.laven.repository;

import iuh.fit.laven.model.Variant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, String> {
    // Nạp cả danh mục để tính giá bán từ phí đóng gói sau khi truy vấn.
    @EntityGraph(attributePaths = "product.category")
    List<Variant> findByProductProductId(String productId);

    @EntityGraph(attributePaths = "product.category")
    List<Variant> findByProductProductIdIn(Collection<String> productIds);

    @EntityGraph(attributePaths = "product.category")

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

    @EntityGraph(attributePaths = "product.category")
    Optional<Variant> findByVariantId(String variantId);

    /** Chatbot: biến thể của sản phẩm đang bán, có đủ số đo để so với quy định hành lý. */
    @EntityGraph(attributePaths = "product.category")
    @Query("""
        SELECT v FROM Variant v
        WHERE v.product.status = true AND v.product.category.status = true
          AND v.length IS NOT NULL AND v.width IS NOT NULL
          AND v.height IS NOT NULL AND v.weight IS NOT NULL
        """)
    List<Variant> findForTrip();
}
