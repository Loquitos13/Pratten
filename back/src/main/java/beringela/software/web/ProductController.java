package beringela.software.web;

import beringela.software.dto.CatalogDtos.ProductRequest;
import beringela.software.dto.CatalogDtos.ProductResponse;
import beringela.software.dto.CatalogDtos.StockAdjustmentRequest;
import beringela.software.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER')")
    public List<ProductResponse> list() {
        return catalogService.findProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER')")
    public List<ProductResponse> lowStock() {
        return catalogService.findLowStock().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER')")
    public ProductResponse get(@PathVariable UUID id) {
        return ProductResponse.from(catalogService.getProduct(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(catalogService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(catalogService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ProductResponse adjustStock(@PathVariable UUID id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ProductResponse.from(catalogService.adjustStock(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public void delete(@PathVariable UUID id) {
        catalogService.deleteProduct(id);
    }
}
