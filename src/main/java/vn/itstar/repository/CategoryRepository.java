package vn.itstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.itstar.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByCategoryNameContainingIgnoreCase(String keyword, Pageable pageable);
    boolean existsByCategoryNameIgnoreCase(String name);
    boolean existsByCategoryNameIgnoreCaseAndIdNot(String name, Long id);
}
