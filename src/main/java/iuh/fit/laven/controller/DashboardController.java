package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.DashboardResponse;
import iuh.fit.laven.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboard;

    @GetMapping("/api/staff/dashboard")
    public DashboardResponse getDashboard() {
        return dashboard.getDashboard();
    }
}
