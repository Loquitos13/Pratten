package beringela.software.repository;

import beringela.software.domain.RemoteSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemoteSessionRepository extends JpaRepository<RemoteSession, UUID> {

    Optional<RemoteSession> findByIdAndActiveTrue(UUID id);

    List<RemoteSession> findByTenantIdAndActiveTrueOrderByStartedAtDesc(UUID tenantId);

    List<RemoteSession> findByPlatformAdminIdAndActiveTrueOrderByStartedAtDesc(UUID adminId);

    long countByActiveTrue();
}
