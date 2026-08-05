package beringela.software.web;

import beringela.software.dto.CatalogDtos.MenuItemIngredientRequest;
import beringela.software.dto.CatalogDtos.MenuItemIngredientResponse;
import beringela.software.dto.CatalogDtos.MenuItemRequest;
import beringela.software.dto.CatalogDtos.MenuItemResponse;
import beringela.software.service.CatalogService;
import beringela.software.service.StockService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu-items")
public class MenuItemController {

    private final CatalogService catalogService;
    private final StockService stockService;

    public MenuItemController(CatalogService catalogService, StockService stockService) {
        this.catalogService = catalogService;
        this.stockService = stockService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER')")
    public List<MenuItemResponse> list(
            @RequestParam(name = "availableOnly", defaultValue = "false") boolean availableOnly) {
        var items = availableOnly ? catalogService.findAvailableMenuItems() : catalogService.findMenuItems();
        return items.stream().map(MenuItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','WAITER')")
    public MenuItemResponse get(@PathVariable UUID id) {
        return MenuItemResponse.from(catalogService.getMenuItem(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public MenuItemResponse create(@Valid @RequestBody MenuItemRequest request) {
        return MenuItemResponse.from(catalogService.createMenuItem(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public MenuItemResponse update(@PathVariable UUID id, @Valid @RequestBody MenuItemRequest request) {
        return MenuItemResponse.from(catalogService.updateMenuItem(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public void delete(@PathVariable UUID id) {
        catalogService.deleteMenuItem(id);
    }

    @GetMapping("/{id}/ingredients")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public List<MenuItemIngredientResponse> recipe(@PathVariable UUID id) {
        return stockService.getRecipe(id);
    }

    @PutMapping("/{id}/ingredients")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public List<MenuItemIngredientResponse> setRecipe(@PathVariable UUID id,
            @Valid @RequestBody List<@Valid MenuItemIngredientRequest> ingredients) {
        return stockService.setRecipe(id, ingredients);
    }
}
