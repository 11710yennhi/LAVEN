package iuh.fit.laven.repository;

import iuh.fit.laven.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    @Query("""
        select product from Product product
        where (:includeInactive = true
               or (product.status = true and product.category.status = true))
          and (:keyword is null
               or lower(product.productName) like lower(concat('%', :keyword, '%'))
               or lower(product.brand) like lower(concat('%', :keyword, '%')))
          and (:categoryId is null or product.category.categoryId = :categoryId)
          and (:brand is null or lower(product.brand) = lower(:brand))
          and (:material is null or lower(product.material) = lower(:material))
        """)
    List<Product> search(
        @Param("keyword") String keyword,
        @Param("categoryId") String categoryId,
        @Param("brand") String brand,
        @Param("material") String material,
        @Param("includeInactive") boolean includeInactive
    );
}
