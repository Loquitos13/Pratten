package beringela.software.service;

import beringela.software.common.NotFoundException;
import beringela.software.domain.Category;
import beringela.software.domain.MenuItem;
import beringela.software.domain.Product;
import beringela.software.dto.CatalogDtos.CategoryRequest;
import beringela.software.dto.CatalogDtos.MenuItemRequest;
import beringela.software.dto.CatalogDtos.ProductRequest;
import beringela.software.dto.CatalogDtos.StockAdjustmentRequest;
import beringela.software.repository.CategoryRepository;
import beringela.software.repository.MenuItemRepository;
import beringela.software.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages the catalog: categories, inventory products and menu items. */
@Service
@Transactional
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final MenuItemRepository menuItemRepository;

    public CatalogService(CategoryRepository categoryRepository, ProductRepository productRepository,
            MenuItemRepository menuItemRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.menuItemRepository = menuItemRepository;
    }

    // ---- Categories ----

    @Transactional(readOnly = true)
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc();
    }

    public Category createCategory(CategoryRequest request) {
        return saveCategory(new Category(), request);
    }

    public Category updateCategory(UUID id, CategoryRequest request) {
        return saveCategory(getCategory(id), request);
    }

    public void deleteCategory(UUID id) {
        categoryRepository.delete(getCategory(id));
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    private Category saveCategory(Category category, CategoryRequest request) {
        category.setName(request.name());
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        return categoryRepository.save(category);
    }

    // ---- Products (stock) ----

    @Transactional(readOnly = true)
    public List<Product> findProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStock() {
        return productRepository.findLowStock();
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> NotFoundException.of("Product", id));
    }

    public Product createProduct(ProductRequest request) {
        return saveProduct(new Product(), request);
    }

    public Product updateProduct(UUID id, ProductRequest request) {
        return saveProduct(getProduct(id), request);
    }

    public void deleteProduct(UUID id) {
        productRepository.delete(getProduct(id));
    }

    public Product adjustStock(UUID id, StockAdjustmentRequest request) {
        Product product = getProduct(id);
        BigDecimal updated = product.getQuantity().add(request.delta());
        product.setQuantity(updated.max(BigDecimal.ZERO));
        return productRepository.save(product);
    }

    private Product saveProduct(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setBarcode(request.barcode());
        product.setCategory(request.categoryId() != null ? getCategory(request.categoryId()) : null);
        product.setUnit(request.unit());
        product.setQuantity(request.quantity());
        product.setMinStock(request.minStock());
        product.setPrice(request.price());
        return productRepository.save(product);
    }

    // ---- Menu items ----

    @Transactional(readOnly = true)
    public List<MenuItem> findMenuItems() {
        return menuItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MenuItem> findAvailableMenuItems() {
        return menuItemRepository.findByAvailableTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public MenuItem getMenuItem(UUID id) {
        return menuItemRepository.findById(id).orElseThrow(() -> NotFoundException.of("MenuItem", id));
    }

    public MenuItem createMenuItem(MenuItemRequest request) {
        return saveMenuItem(new MenuItem(), request);
    }

    public MenuItem updateMenuItem(UUID id, MenuItemRequest request) {
        return saveMenuItem(getMenuItem(id), request);
    }

    public void deleteMenuItem(UUID id) {
        menuItemRepository.delete(getMenuItem(id));
    }

    private MenuItem saveMenuItem(MenuItem item, MenuItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(request.categoryId() != null ? getCategory(request.categoryId()) : null);
        item.setPrice(request.price());
        if (request.available() != null) {
            item.setAvailable(request.available());
        }
        return menuItemRepository.save(item);
    }
}
