package vn.itstar.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.itstar.dto.CategoryDTO;
import vn.itstar.entity.Category;
import vn.itstar.mapper.CategoryMapper;
import vn.itstar.repository.*;
import vn.itstar.services.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;
    private final ProductRepository products;
    private final CategoryMapper mapper;
    private final CategoryImageStorage storage;

    @Override @Transactional(readOnly = true)
    public Page<CategoryDTO> findAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(100, size)),
                Sort.by("id").descending());
        return repository.findByCategoryNameContainingIgnoreCase(
                keyword == null ? "" : keyword.trim(), pageable).map(mapper::toDTO);
    }

    @Override @Transactional(readOnly = true)
    public CategoryDTO findById(Long id) { return mapper.toDTO(getCategory(id)); }

    @Override
    public CategoryDTO create(CategoryDTO dto) {
        String name = dto.getCategoryName().trim();
        if (repository.existsByCategoryNameIgnoreCase(name)) {
            throw new ConflictException("Tên danh mục đã tồn tại");
        }
        Category category = new Category();
        category.setCategoryName(name);
        category.setIcon(storage.save(dto.getIconFile()));
        return mapper.toDTO(repository.saveAndFlush(category));
    }

    @Override
    public CategoryDTO update(Long id, CategoryDTO dto) {
        Category category = getCategory(id);
        String name = dto.getCategoryName().trim();
        if (repository.existsByCategoryNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Tên danh mục đã tồn tại");
        }
        category.setCategoryName(name);
        String icon = storage.save(dto.getIconFile());
        if (icon != null) {
            storage.deleteAfterCommit(category.getIcon());
            category.setIcon(icon);
        }
        return mapper.toDTO(repository.saveAndFlush(category));
    }

    @Override
    public void delete(Long id) {
        Category category = getCategory(id);
        if (products.existsByCategoryId(id)) {
            throw new ConflictException("Danh mục đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước khi xóa.");
        }
        repository.delete(category);
        repository.flush();
        storage.deleteAfterCommit(category.getIcon());
    }

    private Category getCategory(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy danh mục có ID: " + id));
    }
}
