package vn.itstar.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.itstar.dto.ProductDTO;
import vn.itstar.services.ProductService;
import vn.itstar.services.ImageStorageException;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final vn.itstar.repository.CategoryRepository categoryRepository;

    @ModelAttribute("categoryChoices")
    public java.util.List<vn.itstar.entity.Category> categories() {
        return categoryRepository.findAll(org.springframework.data.domain.Sort.by("categoryName"));
    }


    @InitBinder("product")
    public void bindProduct(WebDataBinder binder) {
        binder.setAllowedFields("name", "price", "quantity", "description", "categoryId", "imageFiles", "imageFiles[*]");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String uploadTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("uploadError",
                "Ảnh tối đa 5 MB, tổng dữ liệu gửi tối đa 10 MB. Sản phẩm chưa được lưu, vui lòng thử lại.");
        return "redirect:/products";
    }

    // Danh sách + tìm kiếm + phân trang
    @GetMapping
    public String listProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model) {

        page = Math.max(page, 0);
        size = Math.max(1, Math.min(size, 100));

        Page<ProductDTO> productPage = productService.findAll(keyword, page, size);

        model.addAttribute("products", productPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);

        return "products/list";
    }

    // Hiển thị form thêm sản phẩm
    @GetMapping("/create")
    public String createForm(Model model) {

        model.addAttribute("product", new ProductDTO());
        model.addAttribute("formTitle", "Thêm sản phẩm");

        return "products/form";
    }

    // Lưu sản phẩm mới
    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute("product") ProductDTO dto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("formTitle", "Thêm sản phẩm");
            return "products/form";
        }

        // Thêm mới thì để database tự sinh ID
        dto.setId(null);
        try {
            productService.create(dto);
        } catch (ImageStorageException ex) {
            result.rejectValue("imageFiles", "image.invalid", ex.getMessage());
            model.addAttribute("formTitle", "Thêm sản phẩm");
            return "products/form";
        }

        return "redirect:/products";
    }

    // Hiển thị form sửa sản phẩm
    @GetMapping("/edit/{id}")
    public String editForm(
            @PathVariable("id") Long id,
            Model model) {

        ProductDTO product = productService.findById(id);

        model.addAttribute("product", product);
        model.addAttribute("formTitle", "Cập nhật sản phẩm");

        return "products/form";
    }

    // Lưu thay đổi sản phẩm
    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("product") ProductDTO dto,
            BindingResult result,
            Model model) {

        // Giữ ID đúng theo đường dẫn khi hiển thị lại form
        dto.setId(id);
        dto.setImages(productService.findById(id).getImages());

        if (result.hasErrors()) {
            model.addAttribute("formTitle", "Cập nhật sản phẩm");
            return "products/form";
        }

        try {
            productService.update(id, dto);
        } catch (ImageStorageException ex) {
            dto.setImages(productService.findById(id).getImages());
            result.rejectValue("imageFiles", "image.invalid", ex.getMessage());
            model.addAttribute("formTitle", "Cập nhật sản phẩm");
            return "products/form";
        }

        return "redirect:/products";
    }

    // Giữ đường dẫn GET để khớp nút xóa trong ví dụ 1
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Long id) {

        productService.delete(id);

        return "redirect:/products";
    }

    @PostMapping("/image/delete/{imageId}")
    public String deleteImage(@PathVariable Long imageId, RedirectAttributes redirectAttributes) {
        Long productId = productService.deleteImage(imageId);
        redirectAttributes.addFlashAttribute("success", "Xóa hình ảnh thành công");
        return "redirect:/products/edit/" + productId;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    @ResponseBody
    public String notFound(IllegalArgumentException ex) {
        return ex.getMessage();
    }

    @GetMapping("/ajax")
    public String ajaxPage() {
        return "products/ajax";
    }

}
