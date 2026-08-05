package beringela.software.web;

import beringela.software.dto.PlatformDtos.TenantHealthOverview;
import beringela.software.dto.PlatformDtos.TenantHealthResponse;
import beringela.software.service.TenantHealthService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Estado operacional agregado de todos os tenants. */
@RestController
@RequestMapping("/platform/health")
public class PlatformHealthController {

    private final TenantHealthService tenantHealthService;

    public PlatformHealthController(TenantHealthService tenantHealthService) {
        this.tenantHealthService = tenantHealthService;
    }

    @GetMapping
    public TenantHealthOverview overview() {
        return tenantHealthService.overview();
    }

    @GetMapping("/tenants/{tenantId}")
    public TenantHealthResponse tenant(@PathVariable UUID tenantId) {
        return tenantHealthService.healthForTenant(tenantId);
    }
}
