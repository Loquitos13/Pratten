package beringela.software.service;

import beringela.software.domain.Tenant;
import beringela.software.domain.TenantHealthSnapshot;
import beringela.software.domain.TenantHealthStatus;
import beringela.software.dto.PlatformDtos.PlatformAuditEntry;
import beringela.software.dto.PlatformDtos.PlatformDashboardResponse;
import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import beringela.software.dto.PlatformDtos.TenantHealthOverview;
import beringela.software.repository.PlatformAuditLogRepository;
import beringela.software.repository.PlatformNotificationRepository;
import beringela.software.repository.RemoteSessionRepository;
import beringela.software.repository.TenantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlatformDashboardService {

    private final TenantRepository tenantRepository;
    private final TenantHealthService tenantHealthService;
    private final PlatformNotificationRepository notificationRepository;
    private final RemoteSessionRepository remoteSessionRepository;
    private final PlatformAuditLogRepository auditLogRepository;

    public PlatformDashboardService(TenantRepository tenantRepository,
            TenantHealthService tenantHealthService,
            PlatformNotificationRepository notificationRepository,
            RemoteSessionRepository remoteSessionRepository,
            PlatformAuditLogRepository auditLogRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantHealthService = tenantHealthService;
        this.notificationRepository = notificationRepository;
        this.remoteSessionRepository = remoteSessionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public PlatformDashboardResponse dashboard() {
        List<Tenant> tenants = tenantRepository.findAll();
        TenantHealthOverview health = tenantHealthService.overview();

        long activeTenants = tenants.stream().filter(Tenant::isActive).count();

        List<PlatformAuditEntry> audit = auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .limit(10)
                .map(e -> new PlatformAuditEntry(
                        e.getId(), e.getAdminId(), e.getTenantId(),
                        e.getAction(), e.getDetail(), e.getCreatedAt()))
                .toList();

        List<PlatformNotificationResponse> alerts =
                notificationRepository.findTop100ByOrderByCreatedAtDesc().stream()
                        .limit(10)
                        .map(PlatformNotificationResponse::from)
                        .toList();

        return new PlatformDashboardResponse(
                tenants.size(),
                activeTenants,
                health.healthy(),
                health.degraded(),
                health.offline(),
                notificationRepository.countByReadFalse(),
                remoteSessionRepository.countByActiveTrue(),
                audit,
                alerts);
    }
}
