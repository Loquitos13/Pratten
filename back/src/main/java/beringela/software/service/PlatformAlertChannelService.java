package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.AlertChannelType;
import beringela.software.domain.PlatformAlertChannel;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.dto.PlatformDtos.AlertChannelRequest;
import beringela.software.dto.PlatformDtos.AlertChannelResponse;
import beringela.software.repository.PlatformAlertChannelRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlatformAlertChannelService {

    private final PlatformAlertChannelRepository repository;

    public PlatformAlertChannelService(PlatformAlertChannelRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AlertChannelResponse> list() {
        return repository.findAll().stream().map(AlertChannelResponse::from).toList();
    }

    public AlertChannelResponse create(AlertChannelRequest request) {
        return AlertChannelResponse.from(save(new PlatformAlertChannel(), request));
    }

    public AlertChannelResponse update(UUID id, AlertChannelRequest request) {
        PlatformAlertChannel channel = repository.findById(id)
                .orElseThrow(() -> NotFoundException.of("PlatformAlertChannel", id));
        return AlertChannelResponse.from(save(channel, request));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw NotFoundException.of("PlatformAlertChannel", id);
        }
        repository.deleteById(id);
    }

    private PlatformAlertChannel save(PlatformAlertChannel channel, AlertChannelRequest request) {
        channel.setName(request.name().trim());
        channel.setChannelType(AlertChannelType.valueOf(request.channelType()));
        channel.setTarget(request.target().trim());
        if (request.minSeverity() != null) {
            channel.setMinSeverity(PlatformNotificationSeverity.valueOf(request.minSeverity()));
        }
        channel.setEventTypes(request.eventTypes());
        if (request.active() != null) {
            channel.setActive(request.active());
        }
        return repository.save(channel);
    }
}
