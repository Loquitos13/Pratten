package beringela.software.repository;

import beringela.software.domain.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByOrderByDisplayOrderAscNameAsc();
}
