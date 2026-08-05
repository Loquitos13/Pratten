package beringela.software.dto;

import beringela.software.domain.PlatformAdmin;
import beringela.software.domain.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PlatformDtos {

    private PlatformDtos() {
    }

    public record PlatformLoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record PlatformMeResponse(UUID id, String name, String email) {

        public static PlatformMeResponse from(PlatformAdmin admin) {
            return new PlatformMeResponse(admin.getId(), admin.getName(), admin.getEmail());
        }
    }

    public record PlatformAuthResponse(
            String token,
            String tokenType,
            Instant expiresAt,
            PlatformMeResponse admin) {

        public static PlatformAuthResponse of(String token, Instant expiresAt, PlatformMeResponse admin) {
            return new PlatformAuthResponse(token, "Bearer", expiresAt, admin);
        }
    }

    public record PlatformTenantSummary(
            UUID id,
            String name,
            String slug,
            boolean active,
            String currency,
            Instant createdAt,
            long staffCount,
            long tableCount,
            long openOrders,
            String healthStatus,
            Instant lastHeartbeat) {
    }

    public record PlatformTenantDetail(
            UUID id,
            String name,
            String slug,
            String vatNumber,
            String address,
            String currency,
            boolean active,
            String supportNotes,
            Instant createdAt,
            PlatformTenantSummary stats) {

        public static PlatformTenantDetail from(Tenant tenant, PlatformTenantSummary stats) {
            return new PlatformTenantDetail(
                    tenant.getId(), tenant.getName(), tenant.getSlug(),
                    tenant.getVatNumber(), tenant.getAddress(), tenant.getCurrency(),
                    tenant.isActive(), tenant.getSupportNotes(), tenant.getCreatedAt(), stats);
        }
    }

    public record UpdatePlatformTenantRequest(
            String name,
            String vatNumber,
            String address,
            String currency,
            Boolean active,
            String supportNotes) {
    }

    public record CreatePlatformTenantRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{2,50}") String slug,
            String vatNumber,
            String address,
            String currency,
            @NotBlank String ownerName,
            @NotBlank @Email String ownerEmail,
            @NotBlank @Size(min = 8, max = 72) String ownerPassword) {
    }

    public record PlatformStaffView(
            UUID id,
            String name,
            String email,
            String role,
            boolean active) {
    }

    public record ResetStaffPasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record PlatformAuditEntry(
            UUID id,
            UUID adminId,
            UUID tenantId,
            String action,
            String detail,
            Instant at) {
    }

    public record PlatformDiagnostics(
            UUID tenantId,
            String tenantName,
            boolean active,
            long staffCount,
            long activeStaff,
            long tables,
            long openOrders,
            List<PlatformStaffView> staff) {
    }

    public record StartRemoteSessionRequest(
            @NotBlank @Size(max = 500) String reason,
            Integer durationMinutes) {
    }

    public record RemoteSessionResponse(
            UUID sessionId,
            String token,
            Instant expiresAt,
            UUID tenantId,
            String tenantName,
            UUID actingStaffId,
            String actingStaffName,
            int durationMinutes) {
    }

    public record PlatformNotificationResponse(
            UUID id,
            UUID tenantId,
            String severity,
            String eventType,
            String title,
            String message,
            boolean read,
            Instant at) {

        public static PlatformNotificationResponse from(
                beringela.software.domain.PlatformNotification n) {
            return new PlatformNotificationResponse(
                    n.getId(), n.getTenantId(), n.getSeverity().name(),
                    n.getEventType(), n.getTitle(), n.getMessage(),
                    n.isRead(), n.getCreatedAt());
        }
    }

    public record TenantHealthResponse(
            UUID tenantId,
            String tenantName,
            String slug,
            boolean tenantActive,
            beringela.software.domain.TenantHealthStatus status,
            Instant lastHeartbeat,
            int activeSyncConnections,
            long avgLatencyMs,
            long pendingSyncEvents,
            String lastError,
            Instant lastCheckedAt) {

        public static TenantHealthResponse from(
                beringela.software.domain.Tenant tenant,
                beringela.software.domain.TenantHealthSnapshot snapshot) {
            return new TenantHealthResponse(
                    tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.isActive(),
                    snapshot.getStatus(), snapshot.getLastHeartbeat(),
                    snapshot.getActiveSyncConnections(), snapshot.getAvgLatencyMs(),
                    snapshot.getPendingSyncEvents(), snapshot.getLastError(),
                    snapshot.getLastCheckedAt());
        }
    }

    public record TenantHealthOverview(
            long totalTenants,
            long healthy,
            long degraded,
            long offline,
            List<TenantHealthResponse> tenants) {
    }

    public record PlatformDashboardResponse(
            long totalTenants,
            long activeTenants,
            long healthyTenants,
            long degradedTenants,
            long offlineTenants,
            long unreadNotifications,
            long activeRemoteSessions,
            List<PlatformAuditEntry> recentAudit,
            List<PlatformNotificationResponse> recentAlerts) {
    }

    public record PlatformAdminResponse(UUID id, String name, String email, boolean active, Instant createdAt) {

        public static PlatformAdminResponse from(PlatformAdmin admin) {
            return new PlatformAdminResponse(
                    admin.getId(), admin.getName(), admin.getEmail(), admin.isActive(), admin.getCreatedAt());
        }
    }

    public record CreatePlatformAdminRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record UpdatePlatformAdminRequest(
            String name,
            Boolean active) {
    }

    public record ResetPlatformAdminPasswordRequest(
            @NotBlank @Size(min = 12, max = 72) String password) {
    }

    public record AlertChannelRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank String channelType,
            @NotBlank @Size(max = 500) String target,
            String minSeverity,
            String eventTypes,
            Boolean active) {
    }

    public record AlertChannelResponse(
            UUID id,
            String name,
            String channelType,
            String target,
            String minSeverity,
            String eventTypes,
            boolean active) {

        public static AlertChannelResponse from(
                beringela.software.domain.PlatformAlertChannel channel) {
            return new AlertChannelResponse(
                    channel.getId(), channel.getName(), channel.getChannelType().name(),
                    channel.getTarget(), channel.getMinSeverity().name(),
                    channel.getEventTypes(), channel.isActive());
        }
    }

    public record AlertDeliveryResponse(
            UUID id,
            UUID channelId,
            String eventType,
            String status,
            Integer responseCode,
            String errorDetail,
            Instant attemptedAt) {

        public static AlertDeliveryResponse from(
                beringela.software.domain.PlatformAlertDelivery delivery) {
            return new AlertDeliveryResponse(
                    delivery.getId(), delivery.getChannelId(), delivery.getEventType(),
                    delivery.getStatus().name(), delivery.getResponseCode(),
                    delivery.getErrorDetail(), delivery.getAttemptedAt());
        }
    }
}
