package vn.itstar.controllers.api;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import vn.itstar.dto.CategoryDTO;
import vn.itstar.services.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryApiController {
    private final CategoryService service;
    public CategoryApiController(CategoryService service) { this.service = service; }

    @InitBinder("category")
    public void bind(WebDataBinder binder) {
        binder.setAllowedFields("categoryName", "iconFile");
    }

    @GetMapping
    public Map<String, Object> findAll(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponses.page(service.findAll(keyword, Math.max(0, page),
                Math.max(1, Math.min(100, size))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponses.body(true, "Lấy dữ liệu thành công", service.findById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@Valid @ModelAttribute("category") CategoryDTO dto) {
        return ResponseEntity.status(201).body(ApiResponses.body(true, "Thêm thành công", service.create(dto)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id,
            @Valid @ModelAttribute("category") CategoryDTO dto) {
        return ResponseEntity.ok(ApiResponses.body(true, "Cập nhật thành công", service.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponses.body(true, "Xóa thành công", null));
    }

}
