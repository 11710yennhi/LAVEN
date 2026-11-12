package iuh.fit.laven.repository;

import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.ColorStockId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColorStockRepository extends JpaRepository<ColorStock, ColorStockId> {
    List<ColorStock> findByVariantProductProductId(String productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from ColorStock stock where stock.id = :id")
    Optional<ColorStock> findLocked(@Param("id") ColorStockId id);
}
