package beringela.software.repository;

import beringela.software.domain.MenuItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByAvailableTrueOrderByNameAsc();
}
