package beringela.software.integration;

import beringela.software.domain.Category;
import beringela.software.domain.MenuItem;
import beringela.software.domain.MenuItemIngredient;
import beringela.software.domain.Product;
import beringela.software.domain.ProductUnit;
import beringela.software.domain.Tenant;
import beringela.software.repository.CategoryRepository;
import beringela.software.repository.MenuItemIngredientRepository;
import beringela.software.repository.MenuItemRepository;
import beringela.software.repository.ProductRepository;
import beringela.software.repository.TenantRepository;
import beringela.software.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.UUID;

/** Dados mínimos para testes de integração multi-tenant. */
public final class IntegrationTestFixtures {

    private IntegrationTestFixtures() {
    }

    public record TenantCatalog(
            Tenant tenant,
            Product product,
            MenuItem menuItem,
            MenuItemIngredient ingredient) {
    }

    public static TenantCatalog seedTenantCatalog(
            TenantRepository tenants,
            CategoryRepository categories,
            ProductRepository products,
            MenuItemRepository menuItems,
            MenuItemIngredientRepository ingredients) {
        Tenant tenant = tenants.save(buildTenant("it-" + UUID.randomUUID().toString().substring(0, 8)));
        TenantContext.set(tenant.getId());
        try {
            Category category = categories.save(buildCategory("Geral"));
            Product product = products.save(buildProduct(category, "2.500", "1.000"));
            MenuItem menuItem = menuItems.save(buildMenuItem(category, "Prato teste"));
            MenuItemIngredient ingredient = ingredients.save(buildIngredient(menuItem, product, "0.500"));
            return new TenantCatalog(tenant, product, menuItem, ingredient);
        } finally {
            TenantContext.clear();
        }
    }

    public static void inTenant(UUID tenantId, Runnable work) {
        TenantContext.set(tenantId);
        try {
            work.run();
        } finally {
            TenantContext.clear();
        }
    }

    private static Tenant buildTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Restaurante " + slug);
        tenant.setSlug(slug);
        tenant.setActive(true);
        return tenant;
    }

    private static Category buildCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDisplayOrder(1);
        return category;
    }

    private static Product buildProduct(Category category, String quantity, String minStock) {
        Product product = new Product();
        product.setName("Ingrediente teste");
        product.setCategory(category);
        product.setUnit(ProductUnit.KG);
        product.setQuantity(new BigDecimal(quantity));
        product.setMinStock(new BigDecimal(minStock));
        product.setPrice(new BigDecimal("5.00"));
        return product;
    }

    private static MenuItem buildMenuItem(Category category, String name) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        item.setPrice(new BigDecimal("10.00"));
        item.setAvailable(true);
        return item;
    }

    private static MenuItemIngredient buildIngredient(
            MenuItem menuItem, Product product, String quantityPerServing) {
        MenuItemIngredient line = new MenuItemIngredient();
        line.setMenuItem(menuItem);
        line.setProduct(product);
        line.setQuantityPerServing(new BigDecimal(quantityPerServing));
        return line;
    }
}
