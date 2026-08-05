package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.PlatformAdmin;
import beringela.software.dto.PlatformDtos.CreatePlatformAdminRequest;
import beringela.software.dto.PlatformDtos.PlatformAdminResponse;
import beringela.software.dto.PlatformDtos.ResetPlatformAdminPasswordRequest;
import beringela.software.dto.PlatformDtos.UpdatePlatformAdminRequest;
import beringela.software.repository.PlatformAdminRepository;
import beringela.software.security.AuthPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PlatformAdminService {

    private final PlatformAdminRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAuditService auditService;

    public PlatformAdminService(PlatformAdminRepository repository,
            PasswordEncoder passwordEncoder,
            PlatformAuditService auditService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PlatformAdminResponse> list() {
        return repository.findAll().stream().map(PlatformAdminResponse::from).toList();
    }

    public PlatformAdminResponse create(AuthPrincipal actor, CreatePlatformAdminRequest request) {
        assertPlatformAdmin(actor);
        if (repository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BusinessException("Email de superadmin já registado.");
        }
        PlatformAdmin admin = new PlatformAdmin();
        admin.setName(request.name().trim());
        admin.setEmail(request.email().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        repository.save(admin);
        auditService.log(actor, null, "PLATFORM_ADMIN_CREATED", "Admin " + admin.getEmail());
        return PlatformAdminResponse.from(admin);
    }

    public PlatformAdminResponse update(AuthPrincipal actor, UUID id, UpdatePlatformAdminRequest request) {
        assertPlatformAdmin(actor);
        PlatformAdmin admin = get(id);
        if (StringUtils.hasText(request.name())) {
            admin.setName(request.name().trim());
        }
        if (request.active() != null) {
            admin.setActive(request.active());
        }
        repository.save(admin);
        auditService.log(actor, null, "PLATFORM_ADMIN_UPDATED", "Admin " + admin.getEmail());
        return PlatformAdminResponse.from(admin);
    }

    public void resetPassword(AuthPrincipal actor, UUID id, ResetPlatformAdminPasswordRequest request) {
        assertPlatformAdmin(actor);
        PlatformAdmin admin = get(id);
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        repository.save(admin);
        auditService.log(actor, null, "PLATFORM_ADMIN_PASSWORD_RESET", "Admin " + admin.getEmail());
    }

    private PlatformAdmin get(UUID id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("PlatformAdmin", id));
    }

    private void assertPlatformAdmin(AuthPrincipal actor) {
        if (!actor.isPlatformAdmin()) {
            throw new BusinessException("Apenas superadmins podem gerir admins.");
        }
    }
}
