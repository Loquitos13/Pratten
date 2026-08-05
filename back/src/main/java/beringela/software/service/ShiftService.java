package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.WorkShift;
import beringela.software.dto.ShiftDtos.ActiveStaffResponse;
import beringela.software.dto.ShiftDtos.ClockInRequest;
import beringela.software.dto.ShiftDtos.ClockOutRequest;
import beringela.software.dto.ShiftDtos.ShiftResponse;
import beringela.software.repository.StaffMemberRepository;
import beringela.software.repository.WorkShiftRepository;
import beringela.software.security.AuthPrincipal;
import beringela.software.sync.SyncEventType;
import beringela.software.sync.SyncService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShiftService {

    private final WorkShiftRepository shiftRepository;
    private final StaffMemberRepository staffRepository;
    private final SyncService syncService;

    public ShiftService(WorkShiftRepository shiftRepository, StaffMemberRepository staffRepository,
            SyncService syncService) {
        this.shiftRepository = shiftRepository;
        this.staffRepository = staffRepository;
        this.syncService = syncService;
    }

    public ShiftResponse clockIn(AuthPrincipal principal, ClockInRequest request) {
        requireShiftEligible(principal);
        shiftRepository.findByStaffMemberIdAndClockOutAtIsNull(principal.userId())
                .ifPresent(s -> {
                    throw new BusinessException("Já estás em turno. Faz clock out ao sair.");
                });

        StaffMember staff = staffRepository.findById(principal.userId())
                .orElseThrow(() -> NotFoundException.of("StaffMember", principal.userId()));
        if (!staff.isActive()) {
            throw new BusinessException("Conta inactiva.");
        }

        WorkShift shift = new WorkShift();
        shift.setStaffMember(staff);
        shift.setClockInAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            shift.setClockInNotes(request.notes().trim());
        }
        ShiftResponse response = ShiftResponse.from(shiftRepository.save(shift), principal);
        publishActiveStaff();
        return response;
    }

    public ShiftResponse clockOut(AuthPrincipal principal, ClockOutRequest request) {
        requireShiftEligible(principal);
        WorkShift shift = shiftRepository.findByStaffMemberIdAndClockOutAtIsNull(principal.userId())
                .orElseThrow(() -> new BusinessException("Não estás em turno."));
        shift.setClockOutAt(Instant.now());
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            shift.setClockOutNotes(request.notes().trim());
        }
        ShiftResponse response = ShiftResponse.from(shiftRepository.save(shift), principal);
        publishActiveStaff();
        return response;
    }

    @Transactional(readOnly = true)
    public ShiftResponse myShift(AuthPrincipal principal) {
        return shiftRepository.findByStaffMemberIdAndClockOutAtIsNull(principal.userId())
                .map(shift -> ShiftResponse.from(shift, principal))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ActiveStaffResponse> activeStaff(StaffRole role) {
        return shiftRepository.findByClockOutAtIsNullOrderByClockInAtAsc().stream()
                .filter(shift -> role == null || shift.getStaffMember().getRole() == role)
                .map(ActiveStaffResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> history(AuthPrincipal viewer, UUID staffId, Instant from, Instant to) {
        assertCanViewHistory(viewer, staffId);
        return shiftRepository
                .findByStaffMemberIdAndClockInAtBetweenOrderByClockInAtDesc(staffId, from, to)
                .stream()
                .map(shift -> ShiftResponse.from(shift, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireActiveShift(UUID staffId) {
        if (shiftRepository.findByStaffMemberIdAndClockOutAtIsNull(staffId).isEmpty()) {
            throw new BusinessException("Tens de fazer clock in ao entrar no turno.");
        }
    }

    private void assertCanViewHistory(AuthPrincipal viewer, UUID staffId) {
        StaffRole role = viewer.staffRole();
        if (role == StaffRole.OWNER || role == StaffRole.MANAGER) {
            return;
        }
        if (viewer.userId().equals(staffId)) {
            return;
        }
        throw new BusinessException("Sem permissão para ver turnos de outro empregado.");
    }

    private void requireShiftEligible(AuthPrincipal principal) {
        StaffRole role = principal.staffRole();
        if (role != StaffRole.WAITER && role != StaffRole.KITCHEN) {
            throw new BusinessException("Apenas equipa de sala ou cozinha faz clock in/out.");
        }
    }

    private void publishActiveStaff() {
        syncService.publish(SyncEventType.SHIFT_UPDATED, activeStaff(null));
    }
}
