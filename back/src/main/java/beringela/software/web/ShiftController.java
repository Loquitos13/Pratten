package beringela.software.web;

import beringela.software.domain.StaffRole;
import beringela.software.dto.ShiftDtos.ActiveStaffResponse;
import beringela.software.dto.ShiftDtos.ClockInRequest;
import beringela.software.dto.ShiftDtos.ClockOutRequest;
import beringela.software.dto.ShiftDtos.ShiftResponse;
import beringela.software.security.AuthPrincipal;
import beringela.software.service.ShiftService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping("/clock-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse clockIn(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody(required = false) @Valid ClockInRequest request) {
        return shiftService.clockIn(principal, request);
    }

    @PostMapping("/clock-out")
    public ShiftResponse clockOut(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody(required = false) @Valid ClockOutRequest request) {
        return shiftService.clockOut(principal, request);
    }

    @GetMapping("/me")
    public ResponseEntity<ShiftResponse> myShift(@AuthenticationPrincipal AuthPrincipal principal) {
        ShiftResponse shift = shiftService.myShift(principal);
        return shift == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(shift);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER','KITCHEN')")
    public List<ActiveStaffResponse> activeStaff(
            @RequestParam(name = "role", required = false) StaffRole role) {
        return shiftService.activeStaff(role);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER','KITCHEN')")
    public List<ShiftResponse> history(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam UUID staffId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : today;
        Instant fromInstant = start.atStartOfDay(zone).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(zone).toInstant();
        return shiftService.history(principal, staffId, fromInstant, toInstant);
    }
}
