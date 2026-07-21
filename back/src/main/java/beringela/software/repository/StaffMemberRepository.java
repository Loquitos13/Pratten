package beringela.software.repository;

import beringela.software.domain.StaffMember;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    Optional<StaffMember> findByEmailIgnoreCase(String email);
}
