package beringela.software.repository;

import beringela.software.domain.PlatformAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, UUID> {

    List<PlatformAuditLog> findTop50ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<PlatformAuditLog> findTop100ByOrderByCreatedAtDesc();
}
