package iuh.fit.laven.repository;

import iuh.fit.laven.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String categoryName, String categoryId);

    List<Category> findByStatusTrueOrderByCategoryNameAsc();
}
