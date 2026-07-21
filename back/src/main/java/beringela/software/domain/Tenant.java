package beringela.software.domain;

import beringela.software.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A restaurant/organization subscribing to Pratten. This is the multi-tenancy
 * root and is intentionally NOT tenant-scoped (it lives in the shared registry).
 */
@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String vatNumber;

    private String address;

    private String currency = "EUR";

    @Column(nullable = false)
    private boolean active = true;
}
