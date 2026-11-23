package iuh.fit.laven.repository;

import iuh.fit.laven.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
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
}
