package beringela.software.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import beringela.software.common.BusinessException;
import beringela.software.domain.MenuItem;
import beringela.software.domain.MenuItemIngredient;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.domain.Product;
import beringela.software.domain.ProductUnit;
import beringela.software.domain.Tenant;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    MenuItemRepository menuItemRepository;
    @Mock
    MenuItemIngredientRepository ingredientRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    SyncService syncService;
    @Mock
    PlatformEventPublisher eventPublisher;
    @Mock
    TenantRepository tenantRepository;

    StockService stockService;

    UUID menuItemId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stockService = new StockService(
                menuItemRepository, ingredientRepository, productRepository, syncService,
                eventPublisher, tenantRepository);
    }

    @Test
    void deductForServingsSubtractsStockAndPublishesCatalogUpdates() {
        Product bacalhau = product("Bacalhau", ProductUnit.KG, "5.000", "1.000");
        MenuItemIngredient line = ingredient(bacalhau, "0.500");

        when(ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId))
                .thenReturn(List.of(line));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Product> updated = stockService.deductForServings(menuItemId, 2);

        assertThat(updated).hasSize(1);
        assertThat(updated.getFirst().getQuantity()).isEqualByComparingTo("4.000");
        verify(syncService).publish(eq(SyncEventType.CATALOG_UPDATED), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void deductForServingsPublishesLowStockPlatformAlertWhenCrossingThreshold() {
        Product product = product("Azeite", ProductUnit.LITER, "2.000", "1.000");
        MenuItemIngredient line = ingredient(product, "0.500");
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Demo");

        when(ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId))
                .thenReturn(List.of(line));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findById(any())).thenReturn(Optional.of(tenant));

        stockService.deductForServings(menuItemId, 3);

        ArgumentCaptor<PlatformEvent> captor = ArgumentCaptor.forClass(PlatformEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(PlatformEventType.LOW_STOCK);
        assertThat(captor.getValue().severity()).isEqualTo(PlatformNotificationSeverity.WARNING);
    }

    @Test
    void deductForServingsFailsWhenStockInsufficient() {
        Product azeite = product("Azeite", ProductUnit.LITER, "0.100", "1.000");
        MenuItemIngredient line = ingredient(azeite, "0.500");

        when(ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId))
                .thenReturn(List.of(line));

        assertThatThrownBy(() -> stockService.deductForServings(menuItemId, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void deductForServingsSkipsWhenRecipeEmpty() {
        when(ingredientRepository.findByMenuItemIdOrderByProduct_NameAsc(menuItemId))
                .thenReturn(List.of());

        assertThat(stockService.deductForServings(menuItemId, 3)).isEmpty();
    }

    private Product product(String name, ProductUnit unit, String quantity, String minStock) {
        Product product = new Product();
        product.setId(productId);
        product.setName(name);
        product.setUnit(unit);
        product.setQuantity(new BigDecimal(quantity));
        product.setMinStock(new BigDecimal(minStock));
        return product;
    }

    private MenuItemIngredient ingredient(Product product, String quantityPerServing) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);

        MenuItemIngredient line = new MenuItemIngredient();
        line.setMenuItem(menuItem);
        line.setProduct(product);
        line.setQuantityPerServing(new BigDecimal(quantityPerServing));
        return line;
    }
}
