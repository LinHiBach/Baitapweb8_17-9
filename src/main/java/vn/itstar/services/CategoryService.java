package vn.itstar.services;

import org.springframework.data.domain.Page;
import vn.itstar.dto.CategoryDTO;

public interface CategoryService {
    Page<CategoryDTO> findAll(String keyword, int page, int size);
    CategoryDTO findById(Long id);
    CategoryDTO create(CategoryDTO dto);
    CategoryDTO update(Long id, CategoryDTO dto);
    void delete(Long id);
}
