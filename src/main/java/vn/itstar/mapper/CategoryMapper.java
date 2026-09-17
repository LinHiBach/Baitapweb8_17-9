package vn.itstar.mapper;

import org.springframework.stereotype.Component;
import vn.itstar.dto.CategoryDTO;
import vn.itstar.entity.Category;

@Component
public class CategoryMapper {
    public CategoryDTO toDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setCategoryName(category.getCategoryName());
        dto.setIcon(category.getIcon());
        return dto;
    }
}
