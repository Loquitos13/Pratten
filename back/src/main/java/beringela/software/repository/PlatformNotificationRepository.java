package beringela.software.repository;

import beringela.software.domain.PlatformNotification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformNotificationRepository extends JpaRepository<PlatformNotification, UUID> {

    List<PlatformNotification> findTop100ByReadFalseOrderByCreatedAtDesc();

    List<PlatformNotification> findTop100ByOrderByCreatedAtDesc();

    long countByReadFalse();
}
