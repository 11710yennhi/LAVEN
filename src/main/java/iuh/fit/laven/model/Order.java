package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.model.User;
import iuh.fit.laven.model.Promotion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @Column(length = 19, nullable = false, updatable = false)
    private String orderId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customerId", nullable = false)
    private User customer;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String receiverName;

    @NotBlank
    @Pattern(regexp = "^0\\d{9}$")
    @Column(nullable = false, length = 10)
    private String receiverPhone;

    @NotBlank
    @Column(nullable = false)
    private String receiverAddressLine;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String receiverWard;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String receiverCity;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer productTotal = 0;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer shippingFee = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderPromotionId")
    private Promotion orderPromotion;

    @Builder.Default
    @NotNull
    @Column(nullable = false)
    @PositiveOrZero
    private Integer orderDiscount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shippingPromotionId")
    private Promotion shippingPromotion;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer shippingDiscount = 0;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer discountAmount = 0;

    @Builder.Default
    @NotNull
    @PositiveOrZero
    @Column(nullable = false)
    private Integer totalAmount = 0;

    @Column(length = 1000)
    private String note;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OrderStatus orderStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 13)
    private PaymentMethod paymentMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmedBy")
    private User confirmedBy;

    private LocalDateTime confirmedAt;
    @Column(length = 30)
    private String trackingCode;
    private LocalDateTime shippingAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime finalizedAt;

    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void applyAmounts(int productTotal, int shippingFee, int orderDiscount, int shippingDiscount) {
        setProductTotal(productTotal);
        setShippingFee(shippingFee);
        setOrderDiscount(orderDiscount);
        setShippingDiscount(shippingDiscount);
        setDiscountAmount(orderDiscount + shippingDiscount);
        setTotalAmount(productTotal + shippingFee - (orderDiscount + shippingDiscount));
    }

}
