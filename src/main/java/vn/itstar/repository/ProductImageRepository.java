package vn.itstar.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.itstar.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);
    long countByProductId(Long productId);
}
