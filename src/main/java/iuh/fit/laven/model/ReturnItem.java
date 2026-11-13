package iuh.fit.laven.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Entity
@Table(name = "return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnItem {

    @EmbeddedId
    private ReturnItemId id = new ReturnItemId();

    @NotNull
    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;

    @NotNull
    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variantId", nullable = false)
    private Variant variant;

    @NotNull
    @MapsId("colorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colorId", nullable = false)
    private Color color;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer damagedQuantity = 0;

    @Column(length = 500)
    private String inspectionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivedVariantId")
    private Variant receivedVariant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivedColorId")
    private Color receivedColor;
}
