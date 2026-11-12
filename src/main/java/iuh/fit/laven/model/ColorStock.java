package iuh.fit.laven.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "color_stocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ColorStock {
    @EmbeddedId
    private ColorStockId id;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variantId", nullable = false)
    @JsonIgnore
    private Variant variant;

    @MapsId("colorId")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "colorId", nullable = false)
    private Color color;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock = 0;
}
