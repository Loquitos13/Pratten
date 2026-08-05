package beringela.software.integration;

import static org.assertj.core.api.Assertions.assertThat;

import beringela.software.domain.AlertChannelType;
import beringela.software.domain.AlertDeliveryStatus;
import beringela.software.domain.PlatformAlertChannel;
import beringela.software.domain.PlatformNotificationSeverity;
import beringela.software.platform.PlatformEvent;
import beringela.software.platform.PlatformEventProcessor;
import beringela.software.platform.PlatformEventType;
import beringela.software.repository.PlatformAlertChannelRepository;
import beringela.software.repository.PlatformAlertDeliveryRepository;
import beringela.software.repository.PlatformNotificationRepository;
import beringela.software.service.StockService;
import beringela.software.integration.IntegrationTestFixtures.TenantCatalog;
import beringela.software.repository.CategoryRepository;
import beringela.software.repository.MenuItemIngredientRepository;
import beringela.software.repository.MenuItemRepository;
import beringela.software.repository.ProductRepository;
import beringela.software.repository.TenantRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
class PlatformStockAlertIntegrationTest {

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    MenuItemRepository menuItemRepository;
    @Autowired
    MenuItemIngredientRepository ingredientRepository;
    @Autowired
    StockService stockService;
    @Autowired
    PlatformNotificationRepository notificationRepository;
    @Autowired
    PlatformAlertChannelRepository alertChannelRepository;
    @Autowired
    PlatformAlertDeliveryRepository deliveryRepository;
    @Autowired
    PlatformEventProcessor eventProcessor;

    HttpServer webhookServer;
    AtomicInteger webhookCalls = new AtomicInteger();
    String webhookUrl;
    TenantCatalog catalog;

    @BeforeEach
    void setUp() throws IOException {
        notificationRepository.deleteAll();
        alertChannelRepository.deleteAll();
        deliveryRepository.deleteAll();
        productRepository.deleteAll();
        menuItemRepository.deleteAll();
        ingredientRepository.deleteAll();
        categoryRepository.deleteAll();
        tenantRepository.deleteAll();

        webhookServer = HttpServer.create(new InetSocketAddress(0), 0);
        webhookServer.createContext("/webhook", exchange -> {
            webhookCalls.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
        });
        webhookServer.start();
        webhookUrl = "http://localhost:" + webhookServer.getAddress().getPort() + "/webhook";

        PlatformAlertChannel channel = new PlatformAlertChannel();
        channel.setName("Webhook IT");
        channel.setChannelType(AlertChannelType.WEBHOOK);
        channel.setTarget(webhookUrl);
        channel.setMinSeverity(PlatformNotificationSeverity.WARNING);
        channel.setEventTypes("LOW_STOCK");
        channel.setActive(true);
        alertChannelRepository.save(channel);

        catalog = IntegrationTestFixtures.seedTenantCatalog(
                tenantRepository, categoryRepository, productRepository,
                menuItemRepository, ingredientRepository);
    }

    @AfterEach
    void tearDown() {
        if (webhookServer != null) {
            webhookServer.stop(0);
        }
    }

    @Test
    void stockDeductionCreatesNotificationAndWebhookDelivery() {
        IntegrationTestFixtures.inTenant(catalog.tenant().getId(), () ->
                stockService.deductForServings(catalog.menuItem().getId(), 3));

        assertThat(notificationRepository.findAll())
                .anyMatch(n -> "LOW_STOCK".equals(n.getEventType())
                        && n.getTitle().equals("Stock baixo")
                        && n.getMessage().contains("Ingrediente teste"));

        assertThat(webhookCalls.get()).isEqualTo(1);
        assertThat(deliveryRepository.findAll())
                .anyMatch(d -> d.getStatus() == AlertDeliveryStatus.SENT
                        && "LOW_STOCK".equals(d.getEventType()));
    }

    @Test
    void lowStockEventCanBeProcessedDirectly() {
        UUID tenantId = catalog.tenant().getId();
        eventProcessor.process(PlatformEvent.of(
                PlatformEventType.LOW_STOCK,
                tenantId,
                catalog.tenant().getName(),
                PlatformNotificationSeverity.WARNING,
                "Stock baixo",
                "Produto X abaixo do mínimo"));

        assertThat(notificationRepository.count()).isEqualTo(1);
        assertThat(webhookCalls.get()).isEqualTo(1);
    }
}
