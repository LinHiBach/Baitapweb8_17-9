package vn.itstar.mapper;

import org.springframework.stereotype.Component;
import vn.itstar.dto.ProductImageDTO;
import vn.itstar.entity.ProductImage;

@Component
public class ProductImageMapper {
    public ProductImageDTO toDTO(ProductImage entity) {
        if (entity == null) {
            return null;
        }
        return ProductImageDTO.builder()
                .id(entity.getId()).imageUrl(entity.getImageUrl())
                .primary(entity.getPrimary()).displayOrder(entity.getDisplayOrder()).build();
    }
}
