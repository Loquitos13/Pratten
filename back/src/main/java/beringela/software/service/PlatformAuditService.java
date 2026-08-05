package beringela.software.service;

import beringela.software.domain.PlatformAuditLog;
import beringela.software.repository.PlatformAuditLogRepository;
import beringela.software.security.AuthPrincipal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformAuditService {

    private final PlatformAuditLogRepository repository;

    public PlatformAuditService(PlatformAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(AuthPrincipal admin, UUID tenantId, String action, String detail) {
        PlatformAuditLog entry = new PlatformAuditLog();
        entry.setAdminId(admin.userId());
        entry.setTenantId(tenantId);
        entry.setAction(action);
        entry.setDetail(detail);
        repository.save(entry);
    }
}
