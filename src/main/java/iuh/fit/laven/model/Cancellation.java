package iuh.fit.laven.model;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.Promotion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancellations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cancellation {

    @Id
    @Column(nullable = false)
    private String orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String reason;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancelledBy", nullable = false)
    private User cancelledBy;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensationPromotionId")
    private Promotion compensationPromotion;
}
