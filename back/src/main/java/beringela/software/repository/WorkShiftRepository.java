package beringela.software.repository;

import beringela.software.domain.WorkShift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkShiftRepository extends JpaRepository<WorkShift, UUID> {

    Optional<WorkShift> findByStaffMemberIdAndClockOutAtIsNull(UUID staffMemberId);

    List<WorkShift> findByClockOutAtIsNullOrderByClockInAtAsc();

    List<WorkShift> findByStaffMemberIdAndClockInAtBetweenOrderByClockInAtDesc(
            UUID staffMemberId, java.time.Instant from, java.time.Instant to);
}
