package iuh.fit.laven.repository;

import iuh.fit.laven.model.ShipCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShipCostRepository extends JpaRepository<ShipCost, Integer> {
    Optional<ShipCost> findByCityIgnoreCase(String city);

    @Query("""
        SELECT s FROM ShipCost s
        WHERE (:city IS NULL OR LOWER(s.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:minPriceShip IS NULL OR s.priceShip >= :minPriceShip)
          AND (:maxPriceShip IS NULL OR s.priceShip <= :maxPriceShip)
        ORDER BY s.city ASC
        """)
    List<ShipCost> search(
        @Param("city") String city,
        @Param("minPriceShip") Integer minPriceShip,
        @Param("maxPriceShip") Integer maxPriceShip
    );
}
