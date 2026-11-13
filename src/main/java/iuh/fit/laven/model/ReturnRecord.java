package iuh.fit.laven.model;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRecord {

    @Id
    @Column(nullable = false)
    private String orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnType returnType;

    @Enumerated(EnumType.STRING)
    private ReturnReason returnReason;

    @Column(length = 500)
    private String note;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer returnShippingCost = 0;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer refundAmount = 0;

    private String refundBankName;

    private String refundAccountNumber;

    private String refundAccountName;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private LocalDateTime receivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivedBy")
    private User receivedBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
