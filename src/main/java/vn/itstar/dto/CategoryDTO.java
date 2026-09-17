package vn.itstar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter @Setter @NoArgsConstructor
public class CategoryDTO {
    private Long id;
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 200, message = "Tên danh mục tối đa 200 ký tự")
    private String categoryName;
    private String icon;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private MultipartFile iconFile;
}
