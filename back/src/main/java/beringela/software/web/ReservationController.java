package beringela.software.web;

import beringela.software.domain.ReservationStatus;
import beringela.software.dto.ReservationDtos.ReservationRequest;
import beringela.software.dto.ReservationDtos.ReservationResponse;
import beringela.software.dto.ReservationDtos.ReservationStatusRequest;
import beringela.software.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff-facing reservation management (confirm, assign table, seat, etc.). */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponse> list(
            @RequestParam(name = "status", required = false) ReservationStatus status) {
        var reservations = status != null
                ? reservationService.findByStatus(status)
                : reservationService.findAll();
        return reservations.stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable UUID id) {
        return ReservationResponse.from(reservationService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request) {
        return ReservationResponse.from(reservationService.create(request));
    }

    @PutMapping("/{id}")
    public ReservationResponse update(@PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request) {
        return ReservationResponse.from(reservationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ReservationResponse changeStatus(@PathVariable UUID id,
            @Valid @RequestBody ReservationStatusRequest request) {
        return ReservationResponse.from(
                reservationService.changeStatus(id, request.status(), request.tableId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        reservationService.delete(id);
    }
}
