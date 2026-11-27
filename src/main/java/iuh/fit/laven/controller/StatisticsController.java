package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.StatisticsResponse;
import iuh.fit.laven.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC31 - Thống kê (STAFF, ADMIN). */
@RestController
@RequestMapping("/api/staff/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** Không truyền year, month thì lấy tháng hiện tại. top = số sản phẩm bán chạy (mặc định 10). */
    @GetMapping
    public StatisticsResponse getStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer top
    ) {
        return statisticsService.getStatistics(year, month, top);
    }
}
