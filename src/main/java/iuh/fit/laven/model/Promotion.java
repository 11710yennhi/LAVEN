    package iuh.fit.laven.model;

    import iuh.fit.laven.model.enums.DiscountType;
    import iuh.fit.laven.model.enums.PromotionApplyType;
    import jakarta.persistence.Column;
    import jakarta.persistence.Entity;
    import jakarta.persistence.EnumType;
    import jakarta.persistence.Enumerated;
    import jakarta.persistence.FetchType;
    import jakarta.persistence.Id;
    import jakarta.persistence.JoinColumn;
    import jakarta.persistence.ManyToOne;
    import jakarta.persistence.Table;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

    import java.time.LocalDate;

    @Entity
    @Table(name = "promotions")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class Promotion {

        @Id
        @Column(length = 19, nullable = false)
        private String promotionId;

        @Column(length = 100, nullable = false)
        private String name;

        @Column(length = 255)
        private String description;

        @Enumerated(EnumType.STRING)
        @Column(length = 20, nullable = false)
        private PromotionApplyType applyType;

        // Chỉ bắt buộc với ORDER/SHIPPING; PRODUCT để null. Service kiểm tra điều này.
        @Column(length = 20, unique = true)
        private String code;

        @Enumerated(EnumType.STRING)
        @Column(length = 20, nullable = false)
        private DiscountType discountType;

        @Column(nullable = false)
        private Integer discountValue;

        @Builder.Default
        @Column(nullable = false)
        private Integer minOrderValue = 0;

        @Builder.Default
        @Column(nullable = false)
        private Integer maxDiscountValue = 0;

        @Column(nullable = false)
        private LocalDate startDate;

        @Column(nullable = false)
        private LocalDate endDate;

        @Builder.Default
        @Column(nullable = false)
        private boolean status = true;

        // null = mã công khai; khác null = mã riêng (bồi thường)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "customerId")
        private User customer;

        // null = không giới hạn
        private Integer maxOrderUsage;
    }