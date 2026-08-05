package beringela.software.repository;

import beringela.software.domain.PlatformAlertChannel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAlertChannelRepository extends JpaRepository<PlatformAlertChannel, UUID> {

    List<PlatformAlertChannel> findByActiveTrueOrderByNameAsc();
}
