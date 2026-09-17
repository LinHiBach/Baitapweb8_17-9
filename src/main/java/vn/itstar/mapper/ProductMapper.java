package vn.itstar.mapper;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import vn.itstar.dto.ProductDTO;
import vn.itstar.entity.Product;

@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final ProductImageMapper imageMapper;

    public ProductDTO toDTO(Product entity) {
        if (entity == null) {
            return null;
        }
        return ProductDTO.builder()
                .id(entity.getId()).name(entity.getName()).price(entity.getPrice())
                .categoryId(entity.getCategory() == null ? null : entity.getCategory().getId())
                .categoryName(entity.getCategory() == null ? null : entity.getCategory().getCategoryName())
                .quantity(entity.getQuantity()).description(entity.getDescription())
                .images(entity.getImages().stream().map(imageMapper::toDTO).toList()).build();
    }

    public Product toEntity(ProductDTO dto) {
        if (dto == null) {
            return null;
        }
        return Product.builder()
                .id(dto.getId()).name(dto.getName()).price(dto.getPrice())
                .quantity(dto.getQuantity()).description(dto.getDescription()).build();
    }

    public void updateEntity(ProductDTO dto, Product entity) {
        entity.setName(dto.getName());
        entity.setPrice(dto.getPrice());
        entity.setQuantity(dto.getQuantity());
        entity.setDescription(dto.getDescription());
    }
}
