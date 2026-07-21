package beringela.software.dto;

import beringela.software.domain.Category;
import beringela.software.domain.MenuItem;
import beringela.software.domain.Product;
import beringela.software.domain.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    // ---- Category ----

    public record CategoryRequest(@NotBlank String name, Integer displayOrder) {
    }

    public record CategoryResponse(UUID id, String name, int displayOrder) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getDisplayOrder());
        }
    }

    // ---- Product (stock) ----

    public record ProductRequest(
            @NotBlank String name,
            String barcode,
            UUID categoryId,
            @NotNull ProductUnit unit,
            @NotNull @PositiveOrZero BigDecimal quantity,
            @NotNull @PositiveOrZero BigDecimal minStock,
            @NotNull @PositiveOrZero BigDecimal price) {
    }

    public record StockAdjustmentRequest(@NotNull BigDecimal delta, String reason) {
    }

    public record ProductResponse(
            UUID id,
            String name,
            String barcode,
            UUID categoryId,
            String categoryName,
            ProductUnit unit,
            BigDecimal quantity,
            BigDecimal minStock,
            BigDecimal price,
            boolean lowStock) {

        public static ProductResponse from(Product p) {
            Category c = p.getCategory();
            return new ProductResponse(p.getId(), p.getName(), p.getBarcode(),
                    c != null ? c.getId() : null, c != null ? c.getName() : null,
                    p.getUnit(), p.getQuantity(), p.getMinStock(), p.getPrice(), p.isLowStock());
        }
    }

    // ---- Menu item ----

    public record MenuItemRequest(
            @NotBlank String name,
            String description,
            UUID categoryId,
            @NotNull @PositiveOrZero BigDecimal price,
            Boolean available) {
    }

    public record MenuItemResponse(
            UUID id,
            String name,
            String description,
            UUID categoryId,
            String categoryName,
            BigDecimal price,
            boolean available) {

        public static MenuItemResponse from(MenuItem m) {
            Category c = m.getCategory();
            return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(),
                    c != null ? c.getId() : null, c != null ? c.getName() : null,
                    m.getPrice(), m.isAvailable());
        }
    }
}
