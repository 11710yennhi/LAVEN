package iuh.fit.laven.repository;

import iuh.fit.laven.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String categoryName, String categoryId);

    List<Category> findByStatusTrueOrderByCategoryNameAsc();

    @Query("""
        SELECT c FROM Category c
        WHERE (:includeInactive = true OR c.status = true)
          AND (:keyword IS NULL
               OR LOWER(c.categoryId) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryName IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :categoryName, '%')))
          AND (:minPackagingCost IS NULL OR c.packagingCost >= :minPackagingCost)
          AND (:maxPackagingCost IS NULL OR c.packagingCost <= :maxPackagingCost)
          AND (:status IS NULL OR c.status = :status)
        ORDER BY c.categoryName ASC
        """)
    List<Category> search(
        @Param("keyword") String keyword,
        @Param("categoryName") String categoryName,
        @Param("minPackagingCost") Integer minPackagingCost,
        @Param("maxPackagingCost") Integer maxPackagingCost,
        @Param("status") Boolean status,
        @Param("includeInactive") boolean includeInactive
    );
}
