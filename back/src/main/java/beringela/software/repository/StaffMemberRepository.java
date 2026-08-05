package beringela.software.repository;

import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    Optional<StaffMember> findByEmailIgnoreCase(String email);

    List<StaffMember> findByRoleAndActiveTrueOrderByNameAsc(StaffRole role);
}
