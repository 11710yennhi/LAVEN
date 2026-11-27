package iuh.fit.laven.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @Column(length = 20, nullable = false, updatable = false)
    private String productId;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String productName;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String brand;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String origin;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String material;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String waterResistance;

    @Column(length = 100)
    private String wheel;

    @Column(length = 100)
    private String zipper;

    @Column(length = 100)
    private String warranty;

    @Column(nullable = false)
    private boolean status = true;

    @Column(length = 500)
    private String stopSellingReason;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(length = 30)
    private String coverImageId;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer maxQuantityPerOrder = 5;

    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getProductDisplayName() {
        return category.getCategoryName() + " " + brand + " " + productName;
    }

}
