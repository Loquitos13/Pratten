package beringela.software.config;

import beringela.software.domain.Category;
import beringela.software.domain.MenuItem;
import beringela.software.domain.Product;
import beringela.software.domain.ProductUnit;
import beringela.software.domain.Reservation;
import beringela.software.domain.ReservationSource;
import beringela.software.domain.ReservationStatus;
import beringela.software.domain.RestaurantTable;
import beringela.software.domain.StaffMember;
import beringela.software.domain.StaffRole;
import beringela.software.domain.TableStatus;
import beringela.software.domain.Tenant;
import beringela.software.repository.CategoryRepository;
import beringela.software.repository.MenuItemRepository;
import beringela.software.repository.ProductRepository;
import beringela.software.repository.ReservationRepository;
import beringela.software.repository.RestaurantTableRepository;
import beringela.software.repository.StaffMemberRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seeds a demo restaurant (tenant) with catalog, tables and staff so the API is
 * immediately usable in development. Enabled via {@code pratten.seed.enabled}.
 */
@Configuration
@ConditionalOnProperty(name = "pratten.seed.enabled", havingValue = "true")
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_SLUG = "demo";
    private static final String DEMO_PASSWORD = "demo1234";

    @Bean
    ApplicationRunner seedDemoData(TenantRepository tenants, CategoryRepository categories,
            ProductRepository products, MenuItemRepository menuItems,
            RestaurantTableRepository tables, StaffMemberRepository staff,
            ReservationRepository reservations, TransactionTemplate tx,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (tenants.findBySlug(DEMO_SLUG).isPresent()) {
                return;
            }
            Tenant tenant = tx.execute(status -> tenants.save(buildTenant()));
            TenantContext.set(tenant.getId());
            try {
                tx.executeWithoutResult(status ->
                        seed(categories, products, menuItems, tables, staff, reservations, passwordEncoder));
            } finally {
                TenantContext.clear();
            }
            log.info("Seeded demo tenant '{}' (slug '{}', id {})",
                    tenant.getName(), DEMO_SLUG, tenant.getId());
            log.info("Demo logins (POST /auth/login, password '{}'): ana@demo.pt [OWNER], "
                    + "joao@demo.pt [WAITER], maria@demo.pt [KITCHEN]", DEMO_PASSWORD);
        };
    }

    private Tenant buildTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("Restaurante Demo");
        tenant.setSlug(DEMO_SLUG);
        tenant.setVatNumber("500000000");
        tenant.setAddress("Rua Central, Lisboa");
        return tenant;
    }

    private void seed(CategoryRepository categories, ProductRepository products,
            MenuItemRepository menuItems, RestaurantTableRepository tables,
            StaffMemberRepository staff, ReservationRepository reservations,
            PasswordEncoder passwordEncoder) {
        Category peixe = categories.save(category("Peixe", 1));
        Category carnes = categories.save(category("Carnes", 2));
        Category bebidas = categories.save(category("Bebidas", 3));
        Category lacticinios = categories.save(category("Lacticínios", 4));
        Category condimentos = categories.save(category("Condimentos", 5));

        products.save(product("Bacalhau Fresco", "5601234567890", peixe, ProductUnit.KG, 3, 10, "12.50"));
        products.save(product("Azeite Extra Virgem", "5601234567891", condimentos, ProductUnit.LITER, 2, 5, "8.90"));
        products.save(product("Vinho Tinto Douro", "5601234567892", bebidas, ProductUnit.BOTTLE, 4, 12, "15.00"));
        products.save(product("Queijo Serra Estrela", "5601234567893", lacticinios, ProductUnit.UNIT, 1, 6, "9.50"));

        menuItems.save(menuItem("Bacalhau à Brás", peixe, "12.00"));
        menuItems.save(menuItem("Francesinha", carnes, "12.00"));
        menuItems.save(menuItem("Polvo à Lagareiro", peixe, "20.00"));
        menuItems.save(menuItem("Arroz de Marisco", peixe, "15.00"));

        // One manager (OWNER) per tenant; the manager assigns the waiter to the
        // tables he is responsible for.
        staff.save(staffMember("Ana Gestora", "ana@demo.pt", StaffRole.OWNER, passwordEncoder));
        StaffMember waiter = staff.save(staffMember("João Empregado", "joao@demo.pt", StaffRole.WAITER, passwordEncoder));
        staff.save(staffMember("Maria Cozinha", "maria@demo.pt", StaffRole.KITCHEN, passwordEncoder));

        tables.save(table("1", "Salão", 2, waiter));
        tables.save(table("2", "Salão", 4, waiter));
        tables.save(table("3", "Salão", 4, waiter));
        tables.save(table("4", "Esplanada", 2, null));
        tables.save(table("5", "Esplanada", 6, null));

        Reservation reservation = new Reservation();
        reservation.setCustomerName("Cliente Website");
        reservation.setCustomerEmail("cliente@exemplo.pt");
        reservation.setPartySize(4);
        reservation.setReservedAt(Instant.now().plus(1, ChronoUnit.DAYS));
        reservation.setSource(ReservationSource.WEBSITE);
        reservation.setStatus(ReservationStatus.PENDING);
        reservations.save(reservation);
    }

    private Category category(String name, int order) {
        Category c = new Category();
        c.setName(name);
        c.setDisplayOrder(order);
        return c;
    }

    private Product product(String name, String barcode, Category category, ProductUnit unit,
            double quantity, double minStock, String price) {
        Product p = new Product();
        p.setName(name);
        p.setBarcode(barcode);
        p.setCategory(category);
        p.setUnit(unit);
        p.setQuantity(BigDecimal.valueOf(quantity));
        p.setMinStock(BigDecimal.valueOf(minStock));
        p.setPrice(new BigDecimal(price));
        return p;
    }

    private MenuItem menuItem(String name, Category category, String price) {
        MenuItem m = new MenuItem();
        m.setName(name);
        m.setCategory(category);
        m.setPrice(new BigDecimal(price));
        return m;
    }

    private RestaurantTable table(String number, String zone, int seats, StaffMember assignedWaiter) {
        RestaurantTable t = new RestaurantTable();
        t.setNumber(number);
        t.setZone(zone);
        t.setSeats(seats);
        t.setStatus(TableStatus.FREE);
        t.setAssignedWaiter(assignedWaiter);
        return t;
    }

    private StaffMember staffMember(String name, String email, StaffRole role,
            PasswordEncoder passwordEncoder) {
        StaffMember s = new StaffMember();
        s.setName(name);
        s.setEmail(email);
        s.setRole(role);
        s.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        return s;
    }
}
