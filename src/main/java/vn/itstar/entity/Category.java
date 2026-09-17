package vn.itstar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter @Setter @NoArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "category_name", nullable = false, unique = true, length = 200,
            columnDefinition = "NVARCHAR(200)")
    private String categoryName;
    @Column(length = 500)
    private String icon;
}
