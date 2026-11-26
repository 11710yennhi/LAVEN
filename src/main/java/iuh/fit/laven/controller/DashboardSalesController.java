package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.DashboardSalesResponse;
import iuh.fit.laven.service.DashboardSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardSalesController {
    private final DashboardSalesService sales;

    @GetMapping(value = "/api/staff/dashboard/sales", produces = MediaType.APPLICATION_JSON_VALUE)
    public DashboardSalesResponse getSales(@RequestParam(defaultValue = "MONTH") DashboardSalesService.Period period) {
        return sales.getSales(period);
    }
}
