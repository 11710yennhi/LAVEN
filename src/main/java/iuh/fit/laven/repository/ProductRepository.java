package iuh.fit.laven.repository;

import iuh.fit.laven.dto.response.CategoryProductPage;
import iuh.fit.laven.model.Product;
import org.springframework.data.domain.Page;
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
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR LOWER(CONCAT(p.brand, ' ', p.productName)) LIKE :keyword ESCAPE '!'
               OR LOWER(p.productId) LIKE :keyword ESCAPE '!')
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:status IS NULL OR p.status = :status)
          AND (:prefix IS NULL OR p.productId LIKE :prefix ESCAPE '!')
          AND (:comingSoon IS NULL
               OR (:comingSoon = true AND p.status = true AND NOT EXISTS (
                   SELECT 1 FROM ColorStock s WHERE s.variant.product = p
               ))
               OR (:comingSoon = false AND (p.status = false OR EXISTS (
                   SELECT 1 FROM ColorStock s WHERE s.variant.product = p
               ))))
          AND (:lowStock = false OR (p.status = true AND p.category.status = true AND EXISTS (
              SELECT 1 FROM ColorStock s WHERE s.variant.product = p AND s.stock <= :threshold
          )))
        ORDER BY p.productId DESC
        """)
    Page<Product> searchStaffPage(
        @Param("keyword") String keyword, @Param("categoryId") String categoryId,
        @Param("status") Boolean status, @Param("prefix") String prefix,
        @Param("comingSoon") Boolean comingSoon, @Param("lowStock") boolean lowStock,
        @Param("threshold") int threshold, Pageable pageable
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
