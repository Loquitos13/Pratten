package beringela.software.repository;

import beringela.software.domain.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("select p from Product p where p.quantity <= p.minStock order by p.name asc")
    List<Product> findLowStock();
}
