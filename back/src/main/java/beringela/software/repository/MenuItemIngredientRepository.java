package beringela.software.repository;

import beringela.software.domain.MenuItemIngredient;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, UUID> {

    List<MenuItemIngredient> findByMenuItemIdOrderByProduct_NameAsc(UUID menuItemId);

    void deleteByMenuItemId(UUID menuItemId);
}
