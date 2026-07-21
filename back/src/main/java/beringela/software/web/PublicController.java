package beringela.software.web;

import beringela.software.dto.CatalogDtos.MenuItemResponse;
import beringela.software.dto.PublicDtos.AvailabilityResponse;
import beringela.software.dto.ReservationDtos.PublicReservationRequest;
import beringela.software.dto.ReservationDtos.ReservationResponse;
import beringela.software.service.CatalogService;
import beringela.software.service.ReservationService;
import beringela.software.service.TableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints consumed by the restaurant's public website. Tenant-scoped like the
 * rest of the API (the website is configured with its {@code X-Tenant-ID}), it
 * exposes only the safe, read-mostly surface: menu, live availability, and
 * reservation intake. Availability stays in sync with the POS/mobile via the
 * same domain services and the SSE stream.
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    private final CatalogService catalogService;
    private final TableService tableService;
    private final ReservationService reservationService;

    public PublicController(CatalogService catalogService, TableService tableService,
            ReservationService reservationService) {
        this.catalogService = catalogService;
        this.tableService = tableService;
        this.reservationService = reservationService;
    }

    @GetMapping("/menu")
    public List<MenuItemResponse> menu() {
        return catalogService.findAvailableMenuItems().stream().map(MenuItemResponse::from).toList();
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability() {
        return AvailabilityResponse.from(tableService.findAll());
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse book(@Valid @RequestBody PublicReservationRequest request) {
        return ReservationResponse.from(reservationService.createFromWebsite(request));
    }
}
