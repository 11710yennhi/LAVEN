package iuh.fit.laven.controller;

import iuh.fit.laven.model.Airline;
import iuh.fit.laven.model.BaggageRule;
import iuh.fit.laven.model.enums.BaggageType;
import iuh.fit.laven.service.AirlineService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Hãng bay và quy định hành lý: khách xem, admin quản lý. */
@RestController
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class AirlineController {
    private final AirlineService airlineService;

    public AirlineController(AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    // ===================== Public =====================

    @GetMapping("/public/airlines")
    public List<Airline> getPublicAirlines() {
        return airlineService.findActive();
    }

    @GetMapping("/public/airlines/{airlineId}/baggage-rules")
    public List<BaggageRule> getPublicBaggageRules(@PathVariable String airlineId) {
        return airlineService.findRulesOfActiveAirline(airlineId);
    }

    // ===================== Admin: hãng bay =====================

    /** Ví dụ: /api/admin/airlines?airlineName=viet&status=true&verifiedFrom=2026-01-01 */
    @GetMapping("/admin/airlines")
    public List<Airline> searchAirlines(
        @RequestParam(required = false) String airlineName,
        @RequestParam(required = false) Boolean status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedTo
    ) {
        return airlineService.search(airlineName, status, verifiedFrom, verifiedTo);
    }

    @GetMapping("/admin/airlines/{airlineId}")
    public Airline getAirline(@PathVariable String airlineId) {
        return airlineService.findById(airlineId);
    }

    @PostMapping("/admin/airlines")
    public Airline createAirline(@Valid @RequestBody Airline airline) {
        return airlineService.create(airline);
    }

    @PutMapping("/admin/airlines/{airlineId}")
    public Airline updateAirline(@PathVariable String airlineId, @Valid @RequestBody Airline airline) {
        return airlineService.update(airlineId, airline);
    }

    /** Ngừng dùng hãng (không xóa thật). */
    @DeleteMapping("/admin/airlines/{airlineId}")
    public Airline deactivateAirline(@PathVariable String airlineId) {
        return airlineService.deactivate(airlineId);
    }

    // ===================== Admin: quy định hành lý =====================

    @GetMapping("/admin/baggage-rules")
    public List<BaggageRule> searchBaggageRules(
        @RequestParam(required = false) String airlineId,
        @RequestParam(required = false) String airlineName,
        @RequestParam(required = false) BaggageType baggageType,
        @RequestParam(required = false) String fareClass,
        @RequestParam(required = false) Boolean airlineStatus,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate verifiedTo
    ) {
        return airlineService.searchRules(airlineId, airlineName, baggageType, fareClass,
            airlineStatus, verifiedFrom, verifiedTo);
    }

    @GetMapping("/admin/baggage-rules/{baggageRuleId}")
    public BaggageRule getBaggageRule(@PathVariable String baggageRuleId) {
        return airlineService.findRuleById(baggageRuleId);
    }

    @PostMapping("/admin/airlines/{airlineId}/baggage-rules")
    public BaggageRule createBaggageRule(@PathVariable String airlineId, @Valid @RequestBody BaggageRule rule) {
        return airlineService.createRule(airlineId, rule);
    }

    @PutMapping("/admin/baggage-rules/{baggageRuleId}")
    public BaggageRule updateBaggageRule(@PathVariable String baggageRuleId, @Valid @RequestBody BaggageRule rule) {
        return airlineService.updateRule(baggageRuleId, rule);
    }

    @DeleteMapping("/admin/baggage-rules/{baggageRuleId}")
    public void deleteBaggageRule(@PathVariable String baggageRuleId) {
        airlineService.deleteRule(baggageRuleId);
    }
}
