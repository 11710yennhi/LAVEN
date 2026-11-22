package iuh.fit.laven.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
}
