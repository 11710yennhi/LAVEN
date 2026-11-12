package iuh.fit.laven.repository;

import iuh.fit.laven.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    List<ProductImage> findByProductProductId(String productId);
}
