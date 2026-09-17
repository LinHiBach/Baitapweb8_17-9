package vn.itstar.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDTO {
    private Long id;
    private String imageUrl;
    private Boolean primary;
    private Integer displayOrder;
}
