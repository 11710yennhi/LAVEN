package iuh.fit.laven.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_events", indexes = {
    @Index(name = "ix_events_time_product", columnList = "createdAt,productId"),
    @Index(name = "ix_events_user_time", columnList = "userId,createdAt")
}, uniqueConstraints = @UniqueConstraint(name = "uq_event_purchase", columnNames = {"purchaseOrderId", "productId"}))
public class UserEvent {
    public enum Type { VIEW, ADD_TO_CART, PURCHASE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productId")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type eventType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 20)
    private String purchaseOrderId;
}
