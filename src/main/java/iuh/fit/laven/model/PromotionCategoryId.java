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
public class PromotionCategoryId implements Serializable {

    @Column(length = 19, nullable = false)
    private String promotionId;

    @Column(length = 6, nullable = false)
    private String categoryId;
}