package iuh.fit.laven.repository;

import iuh.fit.laven.model.Variant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariantRepository extends JpaRepository<Variant, String> {
    List<Variant> findByProductProductId(String productId);
}
