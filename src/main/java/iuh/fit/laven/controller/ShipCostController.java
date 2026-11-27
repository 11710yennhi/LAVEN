package iuh.fit.laven.controller;

import org.springframework.http.MediaType;
import iuh.fit.laven.model.ShipCost;
import iuh.fit.laven.service.ShipCostService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShipCostController {
    private final ShipCostService shipCostService;

    public ShipCostController(ShipCostService shipCostService) {
        this.shipCostService = shipCostService;
    }

    @GetMapping("/public/ship-costs")
    public List<ShipCost> searchPublicShipCosts(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Integer minPriceShip,
        @RequestParam(required = false) Integer maxPriceShip
    ) {
        return shipCostService.search(city, minPriceShip, maxPriceShip);
    }

    @GetMapping("/public/ship-costs/{city}")
    public ShipCost getPublicShipCost(@PathVariable String city) {
        return shipCostService.findByCity(city);
    }

    @GetMapping("/staff/ship-costs")
    public List<ShipCost> getShipCosts(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Integer minPriceShip,
        @RequestParam(required = false) Integer maxPriceShip
    ) {
        return shipCostService.search(city, minPriceShip, maxPriceShip);
    }

    @GetMapping("/staff/ship-costs/{shipCostId}")
    public ShipCost getShipCost(@PathVariable int shipCostId) {
        return shipCostService.findById(shipCostId);
    }

    @PostMapping("/staff/ship-costs")
    public ShipCost createShipCost(@Valid @RequestBody ShipCost shipCost) {
        return shipCostService.create(shipCost);
    }

    @PutMapping("/staff/ship-costs/{shipCostId}")
    public ShipCost updateShipCost(
        @PathVariable int shipCostId,
        @Valid @RequestBody ShipCost shipCost
    ) {
        return shipCostService.update(shipCostId, shipCost);
    }
}
