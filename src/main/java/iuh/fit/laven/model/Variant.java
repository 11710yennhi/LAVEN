package iuh.fit.laven.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Variant {
    @Id
    @Column(length = 20, updatable = false)
    private String variantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productId", nullable = false)
    @JsonIgnore
    private Product product;

    @NotBlank
    @Column(nullable = false, length = 60)
    private String size;

    @PositiveOrZero
    private Double weight;

    @PositiveOrZero
    private Double capacity;

    @PositiveOrZero
    private Double length;

    @PositiveOrZero
    private Double width;

    @PositiveOrZero
    private Double height;

    /** Giá nhập theo size. Chỉ nhận khi ghi (request body), không bao giờ trả ra JSON của entity. */
    @NotNull(message = "Giá nhập không được rỗng")
    @Min(value = 1001, message = "Giá nhập phải lớn hơn 1.000 đồng")
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Integer originalPrice;

    /**
     * Giá bán = ceil((originalPrice + category.packagingCost) / 0,7) làm tròn lên nghìn.
     * Cần product đã được nạp (đang trong transaction hoặc đã JOIN FETCH), vì Variant.product là LAZY.
     */
    @Transient
    @JsonIgnore
    public Integer getSalePrice() {
        return product == null ? null : product.calcSalePrice(originalPrice);
    }
}
