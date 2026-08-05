package beringela.software.repository;

import beringela.software.domain.TenantHealthSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantHealthSnapshotRepository extends JpaRepository<TenantHealthSnapshot, UUID> {

    Optional<TenantHealthSnapshot> findByTenantId(UUID tenantId);

    List<TenantHealthSnapshot> findAllByOrderByLastCheckedAtDesc();
}
