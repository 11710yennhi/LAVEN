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
    public List<ShipCost> search(String city, Integer minPriceShip, Integer maxPriceShip) {
        if ((minPriceShip != null && minPriceShip < 0)
            || (maxPriceShip != null && maxPriceShip < 0)
            || (minPriceShip != null && maxPriceShip != null && minPriceShip > maxPriceShip)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng phí vận chuyển không hợp lệ");
        }

        return shipCostRepository.search(blank(city), minPriceShip, maxPriceShip);
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
        // shipCostId là PROVINCE_ID của Viettel Post do staff nhập, không tự sinh.
        if (shipCost.getShipCostId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần nhập mã tỉnh/thành hợp lệ");
        }
        if (shipCostRepository.existsById(shipCost.getShipCostId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã tỉnh/thành đã tồn tại");
        }
        String city = shipCost.getCity().trim();
        if (shipCostRepository.findByCityIgnoreCase(city).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tỉnh/thành đã có phí ship");
        }
        shipCost.setCity(city);
        return shipCostRepository.save(shipCost);
    }

    public ShipCost update(int id, ShipCost request) {
        ShipCost shipCost = findById(id);
        if (request.getShipCostId() != 0 && request.getShipCostId() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không được đổi mã tỉnh/thành");
        }
        // Order lưu receiverCity theo tên. Giữ tên ổn định để đơn cũ còn tra phí ship.
        if (!shipCost.getCity().equals(request.getCity().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không được đổi tên tỉnh/thành đã tạo");
        }
        shipCost.setPriceShip(request.getPriceShip());
        return shipCostRepository.save(shipCost);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Phí vận chuyển không tồn tại");
    }
}
