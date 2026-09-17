package vn.itstar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.itstar.dto.ProductDTO;
import vn.itstar.repository.ProductRepository;
import vn.itstar.services.ImageStorageException;
import vn.itstar.services.ProductImageStorage;
import vn.itstar.services.ProductService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-images;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class Springboot3ApplicationTests {
    @TempDir
    static Path uploads;

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX1cAAAAASUVORK5CYII=");

    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.upload.product-dir", () -> uploads.toString());
        registry.add("app.upload.category-dir", () -> uploads.resolve("categories").toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ProductService service;
    @Autowired vn.itstar.services.CategoryService categoryService;
    @Autowired vn.itstar.repository.CategoryRepository categoryRepository;
    @Autowired ProductRepository repository;
    @Autowired vn.itstar.repository.ProductImageRepository imageRepository;
    @Autowired ProductImageStorage storage;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearProducts() {
        repository.findAll().forEach(product -> service.delete(product.getId()));
        categoryRepository.findAll().forEach(category -> categoryService.delete(category.getId()));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("imageFiles", "../../sample.PNG", "image/png", PNG);
    }

    private ProductDTO product(String name) {
        return ProductDTO.builder().name(name).price(BigDecimal.TEN).quantity(3).build();
    }

    @Test
    void multipleUploadsPersistAndDisplayPrimaryImage() throws Exception {
        mvc.perform(multipart("/products/create").file(image()).file(image())
                .param("name", "Camera").param("price", "10").param("quantity", "3")
                .param("images[0].imageUrl", "forged.png").param("id", "999"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/products"));
        var saved = service.findAll("", 0, 5).getContent().getFirst();
        assertThat(saved.getImages()).hasSize(2);
        assertThat(saved.getImages()).extracting(i -> i.getPrimary()).containsExactly(true, false);
        assertThat(saved.getImages()).extracting(i -> i.getDisplayOrder()).containsExactly(0, 1);
        for (var img : saved.getImages()) {
            assertThat(img.getId()).isNotNull();
            assertThat(img.getImageUrl()).matches("[0-9a-f-]{36}\\.png");
            assertThat(Files.readAllBytes(uploads.resolve(img.getImageUrl()))).isEqualTo(PNG);
            mvc.perform(get("/uploads/products/" + img.getImageUrl()))
                    .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        }
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(saved.getImages().getFirst().getImageUrl())))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(saved.getImages().getLast().getImageUrl()))));
        mvc.perform(get("/products/edit/" + saved.getId())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(saved.getImages().getLast().getImageUrl())));
    }

    @Test
    void updateAppendsAndDeletingPrimaryPromotesNextImage() throws Exception {
        var dto = product("Camera");
        dto.setImageFiles(java.util.List.of(image(), image()));
        var saved = service.create(dto);
        var edit = product("Updated");
        assertThat(service.update(saved.getId(), edit).getImages()).hasSize(2);
        edit.setImageFiles(java.util.List.of(image()));
        var updated = service.update(saved.getId(), edit);
        assertThat(updated.getImages()).hasSize(3);
        var primary = updated.getImages().getFirst();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/products/image/delete/" + primary.getId()))
                .andExpect(redirectedUrl("/products/edit/" + saved.getId()));
        assertThat(uploads.resolve(primary.getImageUrl())).doesNotExist();
        var remaining = service.findById(saved.getId()).getImages();
        assertThat(remaining).extracting(i -> i.getPrimary()).containsExactly(true, false);
        assertThat(remaining).extracting(i -> i.getDisplayOrder()).containsExactly(0, 1);
        service.delete(saved.getId());
        assertThat(imageRepository.count()).isZero();
        for (var img : remaining) {
            assertThat(uploads.resolve(img.getImageUrl())).doesNotExist();
        }
    }

    @Test
    void validationAndInvalidUploadPreserveExistingImages() throws Exception {
        var dto = product("Camera");
        dto.setImageFiles(java.util.List.of(image()));
        var saved = service.create(dto);
        String filename = saved.getImages().getFirst().getImageUrl();
        mvc.perform(multipart("/products/edit/" + saved.getId())
                .param("name", "").param("price", "10").param("quantity", "3"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product", "name"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(filename)));
        mvc.perform(multipart("/products/edit/" + saved.getId()).file(image())
                .file(new MockMultipartFile("imageFiles", "fake.png", "image/png", "not an image".getBytes()))
                .param("name", "Changed").param("price", "10").param("quantity", "3"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("product", "imageFiles"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(filename)));
        assertThat(service.findById(saved.getId()).getName()).isEqualTo("Camera");
        assertThat(service.findById(saved.getId()).getImages()).hasSize(1);
        try (var paths = Files.list(uploads)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).containsExactly(uploads.resolve(filename));
        }
    }

    @Test
    void rejectsUnsupportedAndOversizedFiles() {
        assertThatThrownBy(() -> storage.save(new MockMultipartFile("imageFiles", "x.svg", "image/svg+xml", PNG)))
                .isInstanceOf(ImageStorageException.class);
        assertThatThrownBy(() -> storage.save(new MockMultipartFile("imageFiles", "x.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(ImageStorageException.class).hasMessageContaining("5 MB");
    }

    @Test
    void rollbackRemovesNewFilesAndPreservesDeletedImage() {
        var dto = product("Camera");
        dto.setImageFiles(java.util.List.of(image()));
        var saved = service.create(dto);
        var oldImage = saved.getImages().getFirst();
        var transaction = new TransactionTemplate(transactionManager);
        String replacement = transaction.execute(status -> {
            service.deleteImage(oldImage.getId());
            var edit = product("Changed");
            edit.setImageFiles(java.util.List.of(image()));
            String filename = service.update(saved.getId(), edit).getImages().getFirst().getImageUrl();
            status.setRollbackOnly();
            return filename;
        });
        assertThat(uploads.resolve(replacement)).doesNotExist();
        assertThat(uploads.resolve(oldImage.getImageUrl())).exists();
        assertThat(service.findById(saved.getId()).getImages().getFirst().getId()).isEqualTo(oldImage.getId());
    }

    @Test
    void deletingLastImageThenUploadingCreatesNewPrimary() {
        var dto = product("Camera");
        dto.setImageFiles(java.util.List.of(image()));
        var saved = service.create(dto);
        service.deleteImage(saved.getImages().getFirst().getId());
        assertThat(service.findById(saved.getId()).getImages()).isEmpty();
        var updated = service.update(saved.getId(), dto);
        assertThat(updated.getImages()).hasSize(1);
        assertThat(updated.getImages().getFirst().getPrimary()).isTrue();
        assertThat(updated.getImages().getFirst().getDisplayOrder()).isZero();
    }

    @Test
    void optionalImagesSearchAndPaginationStillWork() throws Exception {
        service.create(product("Camera A"));
        service.create(product("Camera B"));
        service.create(product("Other"));
        var page = service.findAll("camera", 0, 1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent().getFirst().getImages()).isEmpty();
        assertThat(service.findAll("camera", 1, 1).getContent().getFirst().getName()).isEqualTo("Camera A");
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có ảnh")));
        mvc.perform(get("/products/create")).andExpect(status().isOk());
        mvc.perform(get("/products/edit/999999")).andExpect(status().isNotFound());
    }

    @Test
    void categoryApiCrudSearchPaginationAndIconLifecycle() throws Exception {
        mvc.perform(multipart("/api/categories")
                .file(new MockMultipartFile("iconFile", "icon.png", "image/png", PNG))
                .param("categoryName", "Laptop").param("icon", "forged.png").param("id", "999"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.categoryName").value("Laptop"))
                .andExpect(jsonPath("$.data.iconFile").doesNotExist());
        var saved = categoryRepository.findAll().getFirst();
        assertThat(saved.getIcon()).isNotEqualTo("forged.png");
        Path original = uploads.resolve("categories").resolve(saved.getIcon());
        assertThat(original).exists();
        mvc.perform(get("/uploads/categories/" + saved.getIcon()))
                .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/categories/" + saved.getId())
                .param("categoryName", "Laptop Gaming"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.icon").value(saved.getIcon()));
        mvc.perform(multipart("/api/categories").param("categoryName", "Laptop Office"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/categories").param("keyword", " LAPTOP ").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].categoryName").value("Laptop Gaming"));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/categories/" + saved.getId())
                .file(new MockMultipartFile("iconFile", "new.png", "image/png", PNG))
                .param("categoryName", "Laptop Gaming"))
                .andExpect(status().isOk());
        assertThat(original).doesNotExist();
        var updated = categoryRepository.findById(saved.getId()).orElseThrow();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/categories/" + saved.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertThat(uploads.resolve("categories").resolve(updated.getIcon())).doesNotExist();
        mvc.perform(get("/api/categories/" + saved.getId())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void apiValidationConflictAndUnknownIdsReturnJson() throws Exception {
        mvc.perform(multipart("/api/categories").param("categoryName", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.categoryName").exists());
        mvc.perform(multipart("/api/categories").param("categoryName", "Laptop")).andExpect(status().isCreated());
        mvc.perform(multipart("/api/categories").param("categoryName", " laptop "))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(multipart("/api/products").param("name", "").param("price", "-1").param("quantity", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists()).andExpect(jsonPath("$.errors.quantity").exists());
        mvc.perform(get("/api/products/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(multipart("/api/products").param("name", "Test").param("price", "1")
                .param("quantity", "1").param("categoryId", "999999"))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void productApiCrudWithCategoryAndMultipleImages() throws Exception {
        mvc.perform(multipart("/api/categories").param("categoryName", "Camera"))
                .andExpect(status().isCreated());
        Long categoryId = categoryRepository.findAll().getFirst().getId();
        mvc.perform(multipart("/api/products").file(image()).file(image())
                .param("name", "Camera API").param("price", "12.50").param("quantity", "4")
                .param("categoryId", categoryId.toString()).param("images[0].imageUrl", "forged.png"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.images.length()").value(2))
                .andExpect(jsonPath("$.data.categoryName").value("Camera"))
                .andExpect(jsonPath("$.data.imageFiles").doesNotExist());
        Long id = repository.findAll().getFirst().getId();
        mvc.perform(get("/api/products/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(categoryId.intValue()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/categories/" + categoryId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/products/" + id).file(image())
                .param("name", "Camera Updated").param("price", "20").param("quantity", "8")
                .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.images.length()").value(3));
        var saved = service.findById(id);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                "/api/products/images/" + saved.getImages().getFirst().getId()))
                .andExpect(status().isOk());
        assertThat(service.findById(id).getImages().getFirst().getPrimary()).isTrue();
        mvc.perform(get("/api/products").param("keyword", "updated").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Camera Updated"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/products/" + id))
                .andExpect(status().isOk());
        assertThat(imageRepository.count()).isZero();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/categories/" + categoryId))
                .andExpect(status().isOk());
    }

    @Test
    void ajaxPagesAndEmptyOrBoundedPaginationAreAvailable() throws Exception {
        mvc.perform(get("/products/ajax")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/ajax-crud.js")));
        mvc.perform(get("/categories")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("categoryName")));
        mvc.perform(get("/api/categories").param("size", "999").param("page", "-2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.page").value(0)).andExpect(jsonPath("$.data.content").isEmpty());
        mvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    void invalidSecondUploadRollsBackApiCreate() throws Exception {
        long initialFiles;
        try (var files = Files.list(uploads)) { initialFiles = files.filter(Files::isRegularFile).count(); }
        mvc.perform(multipart("/api/products").file(image())
                .file(new MockMultipartFile("imageFiles", "fake.png", "image/png", "invalid".getBytes()))
                .param("name", "Invalid").param("price", "1").param("quantity", "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertThat(repository.count()).isZero();
        try (var files = Files.list(uploads)) {
            assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(initialFiles);
        }
    }
}
