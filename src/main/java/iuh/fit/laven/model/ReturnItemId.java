package iuh.fit.laven.model;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ReturnItemId implements Serializable {
    private String orderId;
    private String variantId;
    private String colorId;

    public ReturnItemId(String orderId, String variantId, String colorId) {
        this.orderId = orderId;
        this.variantId = variantId;
        this.colorId = colorId;
    }
}

