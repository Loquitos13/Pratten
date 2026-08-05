package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.PlatformNotification;
import beringela.software.dto.PlatformDtos.PlatformNotificationResponse;
import beringela.software.repository.PlatformNotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformNotificationService {

    private final PlatformNotificationRepository repository;

    public PlatformNotificationService(PlatformNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PlatformNotificationResponse> listRecent(boolean unreadOnly) {
        List<PlatformNotification> rows = unreadOnly
                ? repository.findTop100ByReadFalseOrderByCreatedAtDesc()
                : repository.findTop100ByOrderByCreatedAtDesc();
        return rows.stream().map(PlatformNotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByReadFalse();
    }

    public void markRead(UUID id) {
        PlatformNotification notification = repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("PlatformNotification", id));
        notification.setRead(true);
        repository.save(notification);
    }

    public void markAllRead() {
        repository.findTop100ByReadFalseOrderByCreatedAtDesc().forEach(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }
}
