package beringela.software.security;



import beringela.software.domain.StaffRole;

import java.util.UUID;



/**

 * Caller autenticado - staff de restaurante, superadmin ou sessão remota de suporte.

 */

public record AuthPrincipal(

        UUID userId,

        String name,

        PrincipalKind kind,

        UUID tenantId,

        StaffRole staffRole,

        UUID remoteSessionId,

        UUID platformAdminId) {



    public static AuthPrincipal staff(UUID userId, UUID tenantId, String name, StaffRole role) {

        return new AuthPrincipal(userId, name, PrincipalKind.STAFF, tenantId, role, null, null);

    }



    public static AuthPrincipal platform(UUID adminId, String name) {

        return new AuthPrincipal(adminId, name, PrincipalKind.PLATFORM, null, null, null, null);

    }



    public static AuthPrincipal remote(UUID ownerStaffId, UUID tenantId, String name,

            UUID remoteSessionId, UUID platformAdminId) {

        return new AuthPrincipal(ownerStaffId, name, PrincipalKind.REMOTE, tenantId,

                StaffRole.OWNER, remoteSessionId, platformAdminId);

    }



    public boolean isPlatformAdmin() {

        return kind == PrincipalKind.PLATFORM;

    }



    public boolean isStaff() {

        return kind == PrincipalKind.STAFF;

    }



    public boolean isRemoteSupport() {

        return kind == PrincipalKind.REMOTE;

    }



    /** OWNER efectivo para autorização (inclui sessões remotas). */

    public boolean actsAsOwner() {

        return staffRole == StaffRole.OWNER || isRemoteSupport();

    }

}

