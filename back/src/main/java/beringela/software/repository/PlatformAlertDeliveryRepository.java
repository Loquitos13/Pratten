package beringela.software.repository;

import beringela.software.domain.PlatformAlertDelivery;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAlertDeliveryRepository extends JpaRepository<PlatformAlertDelivery, UUID> {

    List<PlatformAlertDelivery> findTop50ByChannelIdOrderByAttemptedAtDesc(UUID channelId);
}
