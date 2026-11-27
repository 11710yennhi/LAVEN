package iuh.fit.laven.model.id;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
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

