package beringela.software.config;

import beringela.software.service.TenantHealthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class PlatformSchedulingConfig {

    private final TenantHealthService tenantHealthService;

    public PlatformSchedulingConfig(TenantHealthService tenantHealthService) {
        this.tenantHealthService = tenantHealthService;
    }

    @Scheduled(fixedDelayString = "${pratten.platform.health.check-interval-ms:30000}")
    void evaluateTenantHealth() {
        tenantHealthService.evaluateAllTenants();
    }
}
