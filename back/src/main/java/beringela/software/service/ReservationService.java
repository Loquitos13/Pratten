package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.Reservation;
import beringela.software.domain.ReservationSource;
import beringela.software.domain.ReservationStatus;
import beringela.software.domain.RestaurantTable;
import beringela.software.domain.TableStatus;
import beringela.software.dto.ReservationDtos.PublicReservationRequest;
import beringela.software.dto.ReservationDtos.ReservationRequest;
import beringela.software.dto.ReservationDtos.ReservationResponse;
import beringela.software.dto.TableDtos.TableResponse;
import beringela.software.repository.ReservationRepository;
import beringela.software.repository.RestaurantTableRepository;
import beringela.software.sync.SyncEventType;
import beringela.software.sync.SyncService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository repository;
    private final RestaurantTableRepository tableRepository;
    private final SyncService syncService;

    public ReservationService(ReservationRepository repository,
            RestaurantTableRepository tableRepository, SyncService syncService) {
        this.repository = repository;
        this.tableRepository = tableRepository;
        this.syncService = syncService;
    }

    @Transactional(readOnly = true)
    public List<Reservation> findAll() {
        return repository.findAllByOrderByReservedAtAsc();
    }

    @Transactional(readOnly = true)
    public List<Reservation> findByStatus(ReservationStatus status) {
        return repository.findByStatusOrderByReservedAtAsc(status);
    }

    @Transactional(readOnly = true)
    public List<Reservation> findBetween(Instant start, Instant end) {
        return repository.findByReservedAtBetweenOrderByReservedAtAsc(start, end);
    }

    @Transactional(readOnly = true)
    public Reservation get(UUID id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Reservation", id));
    }

    /** Booking coming from the public website: always starts as PENDING. */
    public Reservation createFromWebsite(PublicReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setCustomerName(request.customerName());
        reservation.setCustomerPhone(request.customerPhone());
        reservation.setCustomerEmail(request.customerEmail());
        reservation.setPartySize(request.partySize());
        reservation.setReservedAt(request.reservedAt());
        reservation.setNotes(request.notes());
        reservation.setSource(ReservationSource.WEBSITE);
        reservation.setStatus(ReservationStatus.PENDING);
        return publish(repository.save(reservation));
    }

    public Reservation create(ReservationRequest request) {
        return saveAndPublish(new Reservation(), request);
    }

    public Reservation update(UUID id, ReservationRequest request) {
        return saveAndPublish(get(id), request);
    }

    public Reservation changeStatus(UUID id, ReservationStatus status, UUID tableId) {
        Reservation reservation = get(id);
        if (tableId != null) {
            reservation.setTable(tableRepository.findById(tableId)
                    .orElseThrow(() -> NotFoundException.of("RestaurantTable", tableId)));
        }
        reservation.setStatus(status);
        applyTableSideEffects(reservation, status);
        return publish(repository.save(reservation));
    }

    public void delete(UUID id) {
        repository.delete(get(id));
    }

    private Reservation saveAndPublish(Reservation reservation, ReservationRequest request) {
        reservation.setCustomerName(request.customerName());
        reservation.setCustomerPhone(request.customerPhone());
        reservation.setCustomerEmail(request.customerEmail());
        reservation.setPartySize(request.partySize());
        reservation.setReservedAt(request.reservedAt());
        if (request.durationMinutes() != null) {
            reservation.setDurationMinutes(request.durationMinutes());
        }
        if (request.source() != null) {
            reservation.setSource(request.source());
        }
        reservation.setNotes(request.notes());
        if (request.tableId() != null) {
            reservation.setTable(tableRepository.findById(request.tableId())
                    .orElseThrow(() -> NotFoundException.of("RestaurantTable", request.tableId())));
        }
        return publish(repository.save(reservation));
    }

    /** Keeps table availability in sync as a reservation moves through its lifecycle. */
    private void applyTableSideEffects(Reservation reservation, ReservationStatus status) {
        RestaurantTable table = reservation.getTable();
        if (table == null) {
            return;
        }
        TableStatus target = switch (status) {
            case CONFIRMED -> TableStatus.RESERVED;
            case SEATED -> TableStatus.OCCUPIED;
            case COMPLETED, CANCELLED, NO_SHOW -> TableStatus.FREE;
            default -> null;
        };
        if (target != null && table.getStatus() != target) {
            table.setStatus(target);
            tableRepository.save(table);
            syncService.publish(SyncEventType.TABLE_UPDATED, TableResponse.from(table));
        }
    }

    private Reservation publish(Reservation reservation) {
        syncService.publish(SyncEventType.RESERVATION_UPDATED, ReservationResponse.from(reservation));
        return reservation;
    }
}
