package vn.itstar.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Dùng lại kiểm tra ảnh và cơ chế dọn file theo transaction, với thư mục riêng.
@Service
public class CategoryImageStorage {
    private final ProductImageStorage storage;
    public CategoryImageStorage(@Value("${app.upload.category-dir:uploads/categories}") String path) {
        storage = new ProductImageStorage(path);
    }
    public String save(MultipartFile file) { return storage.save(file); }
    public void deleteAfterCommit(String filename) { storage.deleteAfterCommit(filename); }
    public String resourceLocation() { return storage.resourceLocation(); }
}
