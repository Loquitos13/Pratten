package beringela.software.dto;

import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.WorkShift;
import beringela.software.security.AuthPrincipal;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ShiftDtos {

    private ShiftDtos() {
    }

    public record ClockInRequest(@Size(max = 500) String notes) {
    }

    public record ClockOutRequest(@Size(max = 500) String notes) {
    }

    public record ShiftResponse(
            UUID id,
            UUID staffId,
            String staffName,
            Instant clockInAt,
            Instant clockOutAt,
            boolean active,
            String clockInNotes,
            String clockOutNotes) {

        public static ShiftResponse from(WorkShift shift, AuthPrincipal viewer) {
            StaffMember staff = shift.getStaffMember();
            boolean showNotes = canViewNotes(viewer, staff.getId());
            return new ShiftResponse(
                    shift.getId(),
                    staff.getId(),
                    staff.getName(),
                    shift.getClockInAt(),
                    shift.getClockOutAt(),
                    shift.isActive(),
                    showNotes ? shift.getClockInNotes() : null,
                    showNotes ? shift.getClockOutNotes() : null);
        }

        /** Resposta sem contexto de viewer - notas omitidas (ex.: listagens públicas). */
        public static ShiftResponse withoutNotes(WorkShift shift) {
            StaffMember staff = shift.getStaffMember();
            return new ShiftResponse(
                    shift.getId(),
                    staff.getId(),
                    staff.getName(),
                    shift.getClockInAt(),
                    shift.getClockOutAt(),
                    shift.isActive(),
                    null,
                    null);
        }

        private static boolean canViewNotes(AuthPrincipal viewer, UUID shiftStaffId) {
            if (viewer == null) {
                return false;
            }
            StaffRole role = viewer.staffRole();
            if (role == StaffRole.OWNER || role == StaffRole.MANAGER) {
                return true;
            }
            return viewer.userId().equals(shiftStaffId);
        }
    }

    /** Empregado em horário, exposto ao POS para selecção de perfil (sem notas). */
    public record ActiveStaffResponse(UUID staffId, String staffName, Instant clockInAt) {

        public static ActiveStaffResponse from(WorkShift shift) {
            return new ActiveStaffResponse(
                    shift.getStaffMember().getId(),
                    shift.getStaffMember().getName(),
                    shift.getClockInAt());
        }
    }
}
