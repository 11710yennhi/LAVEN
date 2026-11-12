package iuh.fit.laven.service;

import iuh.fit.laven.model.ShipCost;
import iuh.fit.laven.repository.ShipCostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class ShipCostService {
    private final ShipCostRepository shipCostRepository;

    public ShipCostService(ShipCostRepository shipCostRepository) {
        this.shipCostRepository = shipCostRepository;
    }

    @Transactional(readOnly = true)
    public List<ShipCost> findAll() {
        return shipCostRepository.findAll().stream()
            .sorted(Comparator.comparing(ShipCost::getCity, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Transactional(readOnly = true)
    public ShipCost findById(int id) {
        return shipCostRepository.findById(id).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public ShipCost findByCity(String city) {
        return shipCostRepository.findByCityIgnoreCase(city).orElseThrow(this::notFound);
    }

    public ShipCost create(ShipCost shipCost) {
        if (shipCost.getShipCostId() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã phí ship do hệ thống sinh");
        }
        if (shipCostRepository.findByCityIgnoreCase(shipCost.getCity()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tỉnh/thành đã có phí ship");
        }
        shipCost.setShipCostId(nextShipCostId());
        return shipCostRepository.save(shipCost);
    }

    public ShipCost update(int id, ShipCost request) {
        ShipCost shipCost = findById(id);
        if (!shipCost.getCity().equalsIgnoreCase(request.getCity())
            && shipCostRepository.existsByCityIgnoreCaseAndShipCostIdNot(request.getCity(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tỉnh/thành đã có phí ship");
        }
        shipCost.setCity(request.getCity());
        shipCost.setPriceShip(request.getPriceShip());
        return shipCostRepository.save(shipCost);
    }

    private int nextShipCostId() {
        return shipCostRepository.findAll().stream()
            .map(ShipCost::getShipCostId)
            .filter(id -> id != null)
            .max(Integer::compareTo)
            .orElse(0) + 1;
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Phí vận chuyển không tồn tại");
    }
}
