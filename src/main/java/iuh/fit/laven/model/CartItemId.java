package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CartItemId implements Serializable {

    @Column(length = 19, nullable = false)
    private String cartId;

    @Column(length = 19, nullable = false)
    private String variantId;

    @Column(length = 7, nullable = false)
    private String colorId;
}