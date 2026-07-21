package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.RestaurantTable;
import beringela.software.domain.StaffMember;
import beringela.software.domain.TableStatus;
import beringela.software.dto.TableDtos.TableRequest;
import beringela.software.dto.TableDtos.TableResponse;
import beringela.software.repository.RestaurantTableRepository;
import beringela.software.repository.StaffMemberRepository;
import beringela.software.sync.SyncEventType;
import beringela.software.sync.SyncService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TableService {

    private final RestaurantTableRepository repository;
    private final StaffMemberRepository staffRepository;
    private final SyncService syncService;

    public TableService(RestaurantTableRepository repository, StaffMemberRepository staffRepository,
            SyncService syncService) {
        this.repository = repository;
        this.staffRepository = staffRepository;
        this.syncService = syncService;
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> findAll() {
        return repository.findAllByOrderByZoneAscNumberAsc();
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> findByWaiter(UUID waiterId) {
        return repository.findByAssignedWaiterIdOrderByNumberAsc(waiterId);
    }

    @Transactional(readOnly = true)
    public RestaurantTable get(UUID id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("RestaurantTable", id));
    }

    public RestaurantTable create(TableRequest request) {
        return saveAndPublish(save(new RestaurantTable(), request));
    }

    public RestaurantTable update(UUID id, TableRequest request) {
        return saveAndPublish(save(get(id), request));
    }

    public RestaurantTable changeStatus(UUID id, TableStatus status) {
        RestaurantTable table = get(id);
        table.setStatus(status);
        return saveAndPublish(table);
    }

    /** Manager assigns (or clears) the waiter responsible for a table. */
    public RestaurantTable assignWaiter(UUID id, UUID waiterId) {
        RestaurantTable table = get(id);
        if (waiterId == null) {
            table.setAssignedWaiter(null);
        } else {
            StaffMember waiter = staffRepository.findById(waiterId)
                    .orElseThrow(() -> NotFoundException.of("StaffMember", waiterId));
            table.setAssignedWaiter(waiter);
        }
        return saveAndPublish(table);
    }

    public void delete(UUID id) {
        repository.delete(get(id));
    }

    private RestaurantTable save(RestaurantTable table, TableRequest request) {
        table.setNumber(request.number());
        table.setZone(request.zone());
        table.setSeats(request.seats());
        if (request.status() != null) {
            table.setStatus(request.status());
        }
        return table;
    }

    private RestaurantTable saveAndPublish(RestaurantTable table) {
        RestaurantTable saved = repository.save(table);
        syncService.publish(SyncEventType.TABLE_UPDATED, TableResponse.from(saved));
        return saved;
    }
}
