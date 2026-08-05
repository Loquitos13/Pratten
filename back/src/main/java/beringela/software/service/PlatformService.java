package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.OrderStatus;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.Tenant;
import beringela.software.dto.PlatformDtos.CreatePlatformTenantRequest;
import beringela.software.dto.PlatformDtos.PlatformAuditEntry;
import beringela.software.dto.PlatformDtos.PlatformDiagnostics;
import beringela.software.dto.PlatformDtos.PlatformStaffView;
import beringela.software.dto.PlatformDtos.PlatformTenantDetail;
import beringela.software.dto.PlatformDtos.PlatformTenantSummary;
import beringela.software.dto.PlatformDtos.UpdatePlatformTenantRequest;
import beringela.software.dto.TenantDtos.CreateTenantRequest;
import beringela.software.domain.TenantHealthSnapshot;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventPublisher;
import beringela.software.platform.PlatformEventType;
import beringela.software.repository.PlatformAuditLogRepository;
import beringela.software.repository.TenantHealthSnapshotRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.security.AuthPrincipal;
import beringela.software.security.LoginAttemptService;
import beringela.software.tenant.TenantSessionExecutor;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import org.hibernate.Session;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PlatformService {

    private static final List<OrderStatus> CLOSED = List.of(OrderStatus.PAID, OrderStatus.CANCELLED);

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final TenantSessionExecutor tenantSessions;
    private final PlatformAuditService auditService;
    private final PlatformAuditLogRepository auditLogRepository;
    private final TenantHealthSnapshotRepository healthRepository;
    private final PlatformEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;

    public PlatformService(TenantRepository tenantRepository, TenantService tenantService,
            TenantSessionExecutor tenantSessions, PlatformAuditService auditService,
            PlatformAuditLogRepository auditLogRepository,
            TenantHealthSnapshotRepository healthRepository,
            PlatformEventPublisher eventPublisher,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttempts) {
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
        this.tenantSessions = tenantSessions;
        this.auditService = auditService;
        this.auditLogRepository = auditLogRepository;
        this.healthRepository = healthRepository;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempts = loginAttempts;
    }

    @Transactional(readOnly = true)
    public List<PlatformTenantSummary> listTenants() {
        return tenantRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformTenantDetail getTenant(UUID tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        return PlatformTenantDetail.from(tenant, toSummary(tenant));
    }

    public PlatformTenantDetail updateTenant(AuthPrincipal admin, UUID tenantId,
            UpdatePlatformTenantRequest request) {
        Tenant tenant = tenantService.get(tenantId);
        if (StringUtils.hasText(request.name())) {
            tenant.setName(request.name());
        }
        if (request.vatNumber() != null) {
            tenant.setVatNumber(request.vatNumber());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }
        if (StringUtils.hasText(request.currency())) {
            tenant.setCurrency(request.currency());
        }
        if (request.active() != null) {
            boolean wasActive = tenant.isActive();
            tenant.setActive(request.active());
            if (wasActive && !request.active()) {
                eventPublisher.publish(PlatformEvent.of(
                        PlatformEventType.TENANT_INACTIVE,
                        tenantId,
                        tenant.getName(),
                        PlatformNotificationSeverity.CRITICAL,
                        "Tenant desactivado",
                        "O restaurante foi suspenso pela plataforma."));
            }
        }
        if (request.supportNotes() != null) {
            tenant.setSupportNotes(request.supportNotes());
        }
        tenantRepository.save(tenant);
        auditService.log(admin, tenantId, "TENANT_UPDATED", "Configuração alterada");
        return getTenant(tenantId);
    }

    public PlatformTenantDetail createTenant(AuthPrincipal admin, CreatePlatformTenantRequest request) {
        Tenant tenant = tenantService.create(new CreateTenantRequest(
                request.name(), request.slug(), request.vatNumber(),
                request.address(), request.currency()));

        tenantSessions.inTenant(tenant.getId(), session -> {
            StaffMember owner = new StaffMember();
            owner.setName(request.ownerName());
            owner.setEmail(request.ownerEmail());
            owner.setRole(StaffRole.OWNER);
            owner.setPasswordHash(passwordEncoder.encode(request.ownerPassword()));
            session.persist(owner);
            return null;
        });

        auditService.log(admin, tenant.getId(), "TENANT_CREATED",
                "Tenant " + tenant.getSlug() + " criado com owner " + request.ownerEmail());
        return getTenant(tenant.getId());
    }

    @Transactional(readOnly = true)
    public PlatformDiagnostics diagnostics(UUID tenantId) {
        Tenant tenant = tenantService.get(tenantId);
        return tenantSessions.inTenant(tenantId, session -> {
            long staffCount = count(session, "select count(s) from StaffMember s");
            long activeStaff = count(session,
                    "select count(s) from StaffMember s where s.active = true");
            long tables = count(session, "select count(t) from RestaurantTable t");
            long openOrders = session.createQuery(
                            "select count(o) from Order o where o.status not in :closed", Long.class)
                    .setParameter("closed", CLOSED)
                    .getSingleResult();

            List<PlatformStaffView> staff = session
                    .createQuery("select s from StaffMember s order by s.name", StaffMember.class)
                    .getResultList().stream()
                    .map(s -> new PlatformStaffView(
                            s.getId(), s.getName(), s.getEmail(),
                            s.getRole().name(), s.isActive()))
                    .toList();

            return new PlatformDiagnostics(
                    tenant.getId(), tenant.getName(), tenant.isActive(),
                    staffCount, activeStaff, tables, openOrders, staff);
        });
    }

    public void resetStaffPassword(AuthPrincipal admin, UUID tenantId, UUID staffId, String password) {
        Tenant tenant = tenantService.get(tenantId);
        tenantSessions.inTenant(tenantId, session -> {
            StaffMember staff = session.find(StaffMember.class, staffId);
            if (staff == null) {
                throw NotFoundException.of("StaffMember", staffId);
            }
            staff.setPasswordHash(passwordEncoder.encode(password));
            session.merge(staff);
            return null;
        });
        auditService.log(admin, tenantId, "STAFF_PASSWORD_RESET",
                "Password reposta para " + staffId);
    }

    public void unlockStaffLogin(AuthPrincipal admin, UUID tenantId, UUID staffId) {
        Tenant tenant = tenantService.get(tenantId);
        StaffMember staff = tenantSessions.inTenant(tenantId, session -> {
            StaffMember s = session.find(StaffMember.class, staffId);
            if (s == null) {
                throw NotFoundException.of("StaffMember", staffId);
            }
            return s;
        });
        if (staff.getEmail() == null) {
            throw new BusinessException("Empregado sem email - não há login a desbloquear.");
        }
        String key = (tenant.getSlug() + ':' + staff.getEmail()).toLowerCase(Locale.ROOT);
        loginAttempts.reset(key);
        auditService.log(admin, tenantId, "STAFF_LOGIN_UNLOCKED",
                "Login desbloqueado para " + staff.getEmail());
    }

    @Transactional(readOnly = true)
    public List<PlatformAuditEntry> auditGlobal() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(e -> new PlatformAuditEntry(
                        e.getId(), e.getAdminId(), e.getTenantId(),
                        e.getAction(), e.getDetail(), e.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlatformAuditEntry> auditForTenant(UUID tenantId) {
        return auditLogRepository.findTop50ByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(e -> new PlatformAuditEntry(
                        e.getId(), e.getAdminId(), e.getTenantId(),
                        e.getAction(), e.getDetail(), e.getCreatedAt()))
                .toList();
    }

    private PlatformTenantSummary toSummary(Tenant tenant) {
        return tenantSessions.inTenant(tenant.getId(), session -> {
            long staff = count(session, "select count(s) from StaffMember s");
            long tables = count(session, "select count(t) from RestaurantTable t");
            long openOrders = session.createQuery(
                            "select count(o) from Order o where o.status not in :closed", Long.class)
                    .setParameter("closed", CLOSED)
                    .getSingleResult();
            TenantHealthSnapshot health = healthRepository.findByTenantId(tenant.getId())
                    .orElse(null);
            String healthStatus = health != null ? health.getStatus().name() : "UNKNOWN";
            Instant lastHeartbeat = health != null ? health.getLastHeartbeat() : null;
            return new PlatformTenantSummary(
                    tenant.getId(), tenant.getName(), tenant.getSlug(),
                    tenant.isActive(), tenant.getCurrency(), tenant.getCreatedAt(),
                    staff, tables, openOrders, healthStatus, lastHeartbeat);
        });
    }

    private long count(Session session, String hql) {
        return session.createQuery(hql, Long.class).getSingleResult();
    }
}
