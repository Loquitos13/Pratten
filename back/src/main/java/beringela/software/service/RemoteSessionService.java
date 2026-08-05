package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.RemoteSession;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.Tenant;
import beringela.software.dto.PlatformDtos.RemoteSessionResponse;
import beringela.software.dto.PlatformDtos.StartRemoteSessionRequest;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventPublisher;
import beringela.software.platform.PlatformEventType;
import beringela.software.repository.RemoteSessionRepository;
import beringela.software.security.AuthPrincipal;
import beringela.software.security.JwtService;
import beringela.software.tenant.TenantSessionExecutor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import beringela.software.domain.PlatformNotificationSeverity;

/** Gestão de sessões remotas - superadmin actua como OWNER num tenant. */
@Service
@Transactional
public class RemoteSessionService {

    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int MAX_DURATION_MINUTES = 480;

    private final RemoteSessionRepository sessionRepository;
    private final TenantService tenantService;
    private final TenantSessionExecutor tenantSessions;
    private final JwtService jwtService;
    private final PlatformAuditService auditService;
    private final PlatformEventPublisher eventPublisher;

    public RemoteSessionService(RemoteSessionRepository sessionRepository,
            TenantService tenantService,
            TenantSessionExecutor tenantSessions,
            JwtService jwtService,
            PlatformAuditService auditService,
            PlatformEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.tenantService = tenantService;
        this.tenantSessions = tenantSessions;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    public RemoteSessionResponse start(AuthPrincipal admin, UUID tenantId,
            StartRemoteSessionRequest request) {
        assertPlatformAdmin(admin);
        Tenant tenant = tenantService.get(tenantId);
        if (!tenant.isActive()) {
            throw new BusinessException("Tenant inactivo - não é possível abrir sessão remota.");
        }

        StaffMember owner = findActiveOwner(tenantId);
        int duration = request.durationMinutes() != null
                ? Math.min(request.durationMinutes(), MAX_DURATION_MINUTES)
                : DEFAULT_DURATION_MINUTES;
        Instant now = Instant.now();
        Instant expires = now.plus(duration, ChronoUnit.MINUTES);

        RemoteSession session = new RemoteSession();
        session.setPlatformAdminId(admin.userId());
        session.setTenantId(tenantId);
        session.setActingStaffId(owner.getId());
        session.setReason(request.reason().trim());
        session.setActive(true);
        session.setStartedAt(now);
        session.setExpiresAt(expires);
        sessionRepository.save(session);

        String token = jwtService.generateRemoteSession(
                admin.userId(), admin.name(), owner, session.getId(), tenantId, expires);

        auditService.log(admin, tenantId, "REMOTE_SESSION_STARTED",
                "Sessão " + session.getId() + ": " + request.reason());

        eventPublisher.publish(PlatformEvent.of(
                PlatformEventType.REMOTE_SESSION_STARTED,
                tenantId,
                tenant.getName(),
                PlatformNotificationSeverity.INFO,
                "Sessão remota iniciada",
                admin.name() + " entrou como owner: " + request.reason()));

        return new RemoteSessionResponse(
                session.getId(), token, expires, tenantId, tenant.getName(),
                owner.getId(), owner.getName(), duration);
    }

    public void end(AuthPrincipal admin, UUID sessionId) {
        assertPlatformAdmin(admin);
        RemoteSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> NotFoundException.of("RemoteSession", sessionId));
        if (!session.isActive()) {
            return;
        }
        session.setActive(false);
        session.setEndedAt(Instant.now());
        sessionRepository.save(session);

        auditService.log(admin, session.getTenantId(), "REMOTE_SESSION_ENDED",
                "Sessão " + sessionId + " encerrada");

        Tenant tenant = tenantService.get(session.getTenantId());
        eventPublisher.publish(PlatformEvent.of(
                PlatformEventType.REMOTE_SESSION_ENDED,
                session.getTenantId(),
                tenant.getName(),
                PlatformNotificationSeverity.INFO,
                "Sessão remota encerrada",
                admin.name() + " saiu do tenant"));
    }

    @Transactional(readOnly = true)
    public List<RemoteSessionResponse> activeForTenant(UUID tenantId) {
        return sessionRepository.findByTenantIdAndActiveTrueOrderByStartedAtDesc(tenantId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isSessionValid(UUID sessionId) {
        return sessionRepository.findByIdAndActiveTrue(sessionId)
                .filter(s -> s.getEndedAt() == null)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    private RemoteSessionResponse toSummary(RemoteSession session) {
        Tenant tenant = tenantService.get(session.getTenantId());
        StaffMember owner = tenantSessions.inTenant(session.getTenantId(),
                s -> s.find(StaffMember.class, session.getActingStaffId()));
        return new RemoteSessionResponse(
                session.getId(), null, session.getExpiresAt(), session.getTenantId(),
                tenant.getName(), session.getActingStaffId(),
                owner != null ? owner.getName() : "Owner",
                (int) ChronoUnit.MINUTES.between(session.getStartedAt(), session.getExpiresAt()));
    }

    private StaffMember findActiveOwner(UUID tenantId) {
        return tenantSessions.inTenant(tenantId, session ->
                session.createQuery(
                                "select s from StaffMember s where s.role = :role and s.active = true "
                                        + "order by s.createdAt asc",
                                StaffMember.class)
                        .setParameter("role", StaffRole.OWNER)
                        .setMaxResults(1)
                        .getResultStream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(
                                "Tenant sem owner activo - cria um owner antes da sessão remota.")));
    }

    private void assertPlatformAdmin(AuthPrincipal admin) {
        if (!admin.isPlatformAdmin()) {
            throw new BusinessException("Apenas superadmins podem gerir sessões remotas.");
        }
    }
}
