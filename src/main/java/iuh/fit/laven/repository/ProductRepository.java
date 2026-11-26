package iuh.fit.laven.repository;

import iuh.fit.laven.dto.response.CategoryProductPage;
import iuh.fit.laven.model.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByProductIdForUpdate(@Param("productId") String productId);

    @Query("""
        SELECT p FROM Product p
        WHERE (:includeInactive = true
               OR (p.status = true AND p.category.status = true))
          AND (:keyword IS NULL
               OR LOWER(p.productId) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.origin) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.material) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:productId IS NULL OR LOWER(p.productId) LIKE LOWER(CONCAT('%', :productId, '%')))
          AND (:productName IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:brand IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%')))
          AND (:origin IS NULL OR LOWER(p.origin) LIKE LOWER(CONCAT('%', :origin, '%')))
          AND (:material IS NULL OR LOWER(p.material) LIKE LOWER(CONCAT('%', :material, '%')))
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.productId DESC
        """)
    List<Product> search(
        @Param("keyword") String keyword,
        @Param("productId") String productId,
        @Param("productName") String productName,
        @Param("categoryId") String categoryId,
        @Param("brand") String brand,
        @Param("origin") String origin,
        @Param("material") String material,
        @Param("status") Boolean status,
        @Param("includeInactive") boolean includeInactive
    );

    @Query("""
        SELECT LOWER(TRIM(p.brand)) AS brandKey, MIN(TRIM(p.brand)) AS brandLabel, p.status AS status,
               COUNT(p) AS productCount,
               SUM(CASE WHEN :brand IS NULL OR LOWER(TRIM(p.brand)) = :brand THEN 1 ELSE 0 END) AS matchingCount
        FROM Product p
        WHERE p.category.categoryId = :categoryId
        GROUP BY LOWER(TRIM(p.brand)), p.status
        """)
    List<CategoryProductCount> countCategoryProducts(@Param("categoryId") String categoryId, @Param("brand") String brand);

    interface CategoryProductCount {
        String getBrandKey();
        String getBrandLabel();
        boolean getStatus();
        long getProductCount();
        long getMatchingCount();
    }

    @Query("""
        SELECT new iuh.fit.laven.dto.response.CategoryProductPage$Item(
            p.productId, p.productName, p.brand, p.status, p.stopSellingReason)
        FROM Product p
        WHERE p.category.categoryId = :categoryId
          AND (:brand IS NULL OR LOWER(TRIM(p.brand)) = :brand)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY LOWER(p.productName), p.productId
        """)
    Slice<CategoryProductPage.Item> findCategoryProducts(
        @Param("categoryId") String categoryId, @Param("brand") String brand,
        @Param("status") Boolean status, Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Product p
        WHERE p.category.categoryId = :categoryId AND p.productId IN :ids
        ORDER BY p.productId
        """)
    List<Product> findSelectedProductsForStatusUpdate(
        @Param("categoryId") String categoryId, @Param("ids") java.util.Set<String> ids
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p FROM Product p
        WHERE p.category.categoryId = :categoryId
          AND (:brand IS NULL OR LOWER(TRIM(p.brand)) = :brand)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.productId
        """)
    List<Product> findCategoryProductsForStatusUpdate(
        @Param("categoryId") String categoryId, @Param("brand") String brand, @Param("status") Boolean status
    );
}
