package beringela.software.web;

import beringela.software.dto.PlatformDtos.PlatformDashboardResponse;
import beringela.software.service.PlatformDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/dashboard")
public class PlatformDashboardController {

    private final PlatformDashboardService dashboardService;

    public PlatformDashboardController(PlatformDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public PlatformDashboardResponse dashboard() {
        return dashboardService.dashboard();
    }
}
