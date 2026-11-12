package iuh.fit.laven.repository;

import iuh.fit.laven.model.ShipCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipCostRepository extends JpaRepository<ShipCost, String> {
    Optional<ShipCost> findByCityIgnoreCase(String city);

    boolean existsByCityIgnoreCaseAndShipCostIdNot(String city, String shipCostId);
}
