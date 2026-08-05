package beringela.software.security;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.Order;
import beringela.software.domain.RestaurantTable;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.repository.StaffMemberRepository;
import beringela.software.service.ShiftService;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolve o empregado a quem fica atribuída uma acção (pedido, pagamento).
 *
 * <p>Clock in é feito uma vez ao entrar no turno; clock out ao sair.
 * Cozinha tem de estar em turno para receber/ver pedidos.
 * Empregado de mesa tem de estar em turno para aceder à API ({@link WaiterShiftFilter}).
 */
@Service
public class StaffActionGuard {

    private final StaffMemberRepository staffRepository;
    private final ShiftService shiftService;

    public StaffActionGuard(StaffMemberRepository staffRepository, ShiftService shiftService) {
        this.staffRepository = staffRepository;
        this.shiftService = shiftService;
    }

    public AuthPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException("Sessão inválida.");
        }
        return principal;
    }

    public StaffMember resolveActingWaiter(UUID requestedWaiterId) {
        AuthPrincipal principal = currentPrincipal();
        if (principal.isRemoteSupport()) {
            return resolveForManagement(requestedWaiterId);
        }
        return switch (principal.staffRole()) {
            case OWNER, MANAGER -> resolveForManagement(requestedWaiterId);
            case WAITER -> resolveForWaiter(principal.userId(), requestedWaiterId);
            default -> throw new BusinessException("Perfil sem permissão para esta acção.");
        };
    }

    /** Cozinha tem de estar em turno para ver/receber pedidos na fila. */
    public void assertKitchenOnShift() {
        AuthPrincipal principal = currentPrincipal();
        if (principal.staffRole() == StaffRole.KITCHEN) {
            shiftService.requireActiveShift(principal.userId());
        }
    }

    /** Garante que o utilizador pode alterar o pedido (empregado de mesa ou gestão). */
    public void assertCanModifyOrder(Order order) {
        AuthPrincipal principal = currentPrincipal();
        if (principal.isRemoteSupport()) {
            return;
        }
        switch (principal.staffRole()) {
            case OWNER, MANAGER -> { }
            case WAITER -> {
                if (order.getWaiter() == null
                        || !order.getWaiter().getId().equals(principal.userId())) {
                    throw new BusinessException("Só podes alterar os teus pedidos.");
                }
            }
            default -> throw new BusinessException("Perfil sem permissão para alterar pedidos.");
        }
    }

    public void assertWaiterCanUseTable(RestaurantTable table, StaffMember waiter) {
        if (table.getAssignedWaiter() != null
                && !table.getAssignedWaiter().getId().equals(waiter.getId())) {
            throw new BusinessException("Esta mesa está atribuída a outro empregado.");
        }
    }

    /** Cozinha em turno para actualizar estado na fila. */
    public void assertKitchenOperator() {
        AuthPrincipal principal = currentPrincipal();
        if (principal.staffRole() != StaffRole.KITCHEN
                && principal.staffRole() != StaffRole.OWNER
                && principal.staffRole() != StaffRole.MANAGER) {
            throw new BusinessException("Apenas a cozinha pode actualizar a fila.");
        }
        assertKitchenOnShift();
    }

    private StaffMember resolveForManagement(UUID requestedWaiterId) {
        if (requestedWaiterId == null) {
            throw new BusinessException("Selecciona o empregado para registar a acção.");
        }
        StaffMember staff = staffRepository.findById(requestedWaiterId)
                .orElseThrow(() -> NotFoundException.of("StaffMember", requestedWaiterId));
        if (!staff.isActive() || staff.getRole() != StaffRole.WAITER) {
            throw new BusinessException("Selecciona um empregado de mesa activo.");
        }
        return staff;
    }

    private StaffMember resolveForWaiter(UUID selfId, UUID requestedWaiterId) {
        if (requestedWaiterId != null && !requestedWaiterId.equals(selfId)) {
            throw new BusinessException("Não podes actuar em nome de outro empregado.");
        }
        return staffRepository.findById(selfId)
                .orElseThrow(() -> NotFoundException.of("StaffMember", selfId));
    }
}
