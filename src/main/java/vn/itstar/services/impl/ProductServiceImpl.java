package vn.itstar.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import vn.itstar.entity.ProductImage;
import vn.itstar.repository.ProductImageRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.itstar.dto.ProductDTO;
import vn.itstar.entity.Product;
import vn.itstar.mapper.ProductMapper;
import vn.itstar.repository.ProductRepository;
import vn.itstar.services.ProductService;
import vn.itstar.services.ProductImageStorage;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final vn.itstar.repository.CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    private final ProductImageStorage imageStorage;
    private final ProductImageRepository imageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> findAll(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").descending());

        Page<Product> products;

        if (keyword == null || keyword.isBlank()) {
            products = productRepository.findAll(pageable);
        } else {
            products = productRepository.findByNameContainingIgnoreCase(
                    keyword.trim(), pageable);
        }

        return products.map(productMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findById(Long id) {
        return productMapper.toDTO(getProduct(id));
    }

    @Override
    public ProductDTO create(ProductDTO dto) {

        dto.setId(null);

        Product product = productMapper.toEntity(dto);
        product.setCreatedAt(LocalDateTime.now());
        setCategory(product, dto.getCategoryId());
        saveImages(product, dto.getImageFiles());

        Product saved = productRepository.save(product);

        return productMapper.toDTO(saved);
    }

    @Override
    public ProductDTO update(Long id, ProductDTO dto) {

        Product product = getProduct(id);

        productMapper.updateEntity(dto, product);
        setCategory(product, dto.getCategoryId());
        saveImages(product, dto.getImageFiles());

        Product updated = productRepository.save(product);

        return productMapper.toDTO(updated);
    }

    @Override
    public void delete(Long id) {
        Product product = getProduct(id);
        productRepository.delete(product);
        product.getImages().forEach(image -> imageStorage.deleteAfterCommit(image.getImageUrl()));
    }

    // Thêm ảnh mới, giữ ảnh cũ và nối tiếp thứ tự hiển thị.
    private void saveImages(Product product, List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        int order = product.getImages().stream().mapToInt(ProductImage::getDisplayOrder)
                .max().orElse(-1) + 1;
        for (MultipartFile file : files) {
            String filename = imageStorage.save(file);
            if (filename == null) {
                continue;
            }
            ProductImage image = ProductImage.builder().product(product).imageUrl(filename)
                    .primary(product.getImages().isEmpty()).displayOrder(order++)
                    .createdAt(LocalDateTime.now()).build();
            product.getImages().add(image);
        }
    }

    @Override
    public Long deleteImage(Long imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new vn.itstar.services.ResourceNotFoundException("Không tìm thấy hình ảnh: " + imageId));
        Product product = image.getProduct();
        product.getImages().remove(image);
        // orphanRemoval xóa bản ghi; chỉ xóa file khi giao dịch commit.
        imageStorage.deleteAfterCommit(image.getImageUrl());
        if (Boolean.TRUE.equals(image.getPrimary()) && !product.getImages().isEmpty()) {
            product.getImages().getFirst().setPrimary(true);
        }
        for (int i = 0; i < product.getImages().size(); i++) {
            product.getImages().get(i).setDisplayOrder(i);
        }
        return product.getId();
    }

    private void setCategory(Product product, Long categoryId) {
        product.setCategory(categoryId == null ? null : categoryRepository.findById(categoryId)
                .orElseThrow(() -> new vn.itstar.services.ResourceNotFoundException(
                        "Không tìm thấy danh mục có ID: " + categoryId)));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new vn.itstar.services.ResourceNotFoundException(
                        "Không tìm thấy sản phẩm có ID: " + id));
    }

}
