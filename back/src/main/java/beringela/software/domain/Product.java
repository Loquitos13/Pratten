package beringela.software.domain;

import beringela.software.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * An inventory/stock item (e.g. an ingredient) with quantity tracking. Distinct
 * from {@link MenuItem}, which is a dish sold to customers.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
public class Product extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductUnit unit = ProductUnit.UNIT;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    public boolean isLowStock() {
        return quantity.compareTo(minStock) <= 0;
    }
}
