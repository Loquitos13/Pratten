package beringela.software.service;

import beringela.software.common.BusinessException;
import beringela.software.common.NotFoundException;
import beringela.software.domain.MenuItem;
import beringela.software.domain.MenuItemIngredient;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.domain.Product;
import beringela.software.dto.CatalogDtos.MenuItemIngredientRequest;
import beringela.software.dto.CatalogDtos.MenuItemIngredientResponse;
import beringela.software.dto.CatalogDtos.ProductResponse;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventPublisher;
import beringela.software.platform.PlatformEventType;
import beringela.software.repository.MenuItemIngredientRepository;
import beringela.software.repository.MenuItemRepository;
import beringela.software.repository.ProductRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.sync.SyncEventType;
import beringela.software.sync.SyncService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Receitas do menu e dedução automática de stock ao enviar para cozinha. */
@Service
@Transactional
public class StockService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemIngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final SyncService syncService;
    private final PlatformEventPublisher eventPublisher;
    private final TenantRepository tenantRepository;

    public StockService(MenuItemRepository menuItemRepository,
            MenuItemIngredientRepository ingredientRepository,
            ProductRepository productRepository,
            SyncService syncService,
            PlatformEventPublisher eventPublisher,
            TenantRepository tenantRepository) {
        this.menuItemRepository = menuItemRepository;
        this.ingredientRepository = ingredientRepository;
        this.productRepository = productRepository;
        this.syncService = syncService;
        this.eventPublisher = eventPublisher;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemIngredientResponse> getRecipe(UUID menuItemId) {
        assertMenuItemExists(menuItemId);
        return ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId).stream()
                .map(MenuItemIngredientResponse::from)
                .toList();
    }

    public List<MenuItemIngredientResponse> setRecipe(UUID menuItemId,
            List<MenuItemIngredientRequest> ingredients) {
        MenuItem menuItem = getMenuItem(menuItemId);
        ingredientRepository.deleteByMenuItemId(menuItemId);

        Set<UUID> seenProducts = new HashSet<>();
        List<MenuItemIngredient> saved = new ArrayList<>();
        for (MenuItemIngredientRequest row : ingredients) {
            if (!seenProducts.add(row.productId())) {
                throw new BusinessException("Produto duplicado na receita.");
            }
            Product product = productRepository.findById(row.productId())
                    .orElseThrow(() -> NotFoundException.of("Product", row.productId()));

            MenuItemIngredient line = new MenuItemIngredient();
            line.setMenuItem(menuItem);
            line.setProduct(product);
            line.setQuantityPerServing(row.quantityPerServing());
            saved.add(ingredientRepository.save(line));
        }

        syncService.publish(SyncEventType.CATALOG_UPDATED,
                java.util.Map.of("entity", "menuItemRecipe", "menuItemId", menuItemId.toString()));
        return saved.stream().map(MenuItemIngredientResponse::from).toList();
    }

    /** Desconta stock com base na receita quando o prato vai para cozinha. */
    public List<Product> deductForServings(UUID menuItemId, int servings) {
        if (servings <= 0) {
            return List.of();
        }
        List<MenuItemIngredient> recipe =
                ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId);
        if (recipe.isEmpty()) {
            return List.of();
        }

        List<Product> updated = new ArrayList<>();
        BigDecimal factor = BigDecimal.valueOf(servings);
        for (MenuItemIngredient line : recipe) {
            Product product = line.getProduct();
            BigDecimal needed = line.getQuantityPerServing().multiply(factor);
            if (product.getQuantity().compareTo(needed) < 0) {
                throw new BusinessException(
                        "Stock insuficiente de \"" + product.getName()
                                + "\" (necessário " + needed + " " + product.getUnit().name()
                                + ", disponível " + product.getQuantity() + ").");
            }
            boolean wasAboveMin = product.getQuantity().compareTo(product.getMinStock()) > 0;
            product.setQuantity(product.getQuantity().subtract(needed));
            Product saved = productRepository.save(product);
            updated.add(saved);
            syncService.publish(SyncEventType.CATALOG_UPDATED, ProductResponse.from(saved));
            if (wasAboveMin && saved.isLowStock()) {
                publishLowStockAlert(saved);
            }
        }
        return updated;
    }

    private void publishLowStockAlert(Product product) {
        tenantRepository.findById(product.getTenantId()).ifPresent(tenant ->
                eventPublisher.publish(PlatformEvent.of(
                        PlatformEventType.LOW_STOCK,
                        tenant.getId(),
                        tenant.getName(),
                        PlatformNotificationSeverity.WARNING,
                        "Stock baixo",
                        product.getName() + ": " + product.getQuantity() + " "
                                + product.getUnit().name() + " (mín. " + product.getMinStock() + ")")));
    }

    private MenuItem getMenuItem(UUID menuItemId) {
        return menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> NotFoundException.of("MenuItem", menuItemId));
    }

    private void assertMenuItemExists(UUID menuItemId) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw NotFoundException.of("MenuItem", menuItemId);
        }
    }
}
