package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.StaffMember;
import beringela.software.dto.StaffDtos.StaffRequest;
import beringela.software.repository.StaffMemberRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class StaffService {

    private final StaffMemberRepository repository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(StaffMemberRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffMember> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public StaffMember get(UUID id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("StaffMember", id));
    }

    public StaffMember create(StaffRequest request) {
        return save(new StaffMember(), request);
    }

    public StaffMember update(UUID id, StaffRequest request) {
        return save(get(id), request);
    }

    public void delete(UUID id) {
        repository.delete(get(id));
    }

    private StaffMember save(StaffMember staff, StaffRequest request) {
        staff.setName(request.name());
        staff.setEmail(request.email());
        staff.setPin(request.pin());
        staff.setRole(request.role());
        if (request.active() != null) {
            staff.setActive(request.active());
        }
        if (StringUtils.hasText(request.password())) {
            staff.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return repository.save(staff);
    }
}
