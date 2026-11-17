package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Giao dịch SePay đã khớp đơn; transactionId giúp bỏ qua webhook gửi lại.
 */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {

    @Id
    @Column(length = 100, nullable = false, updatable = false)
    private String transactionId;

    @Column(length = 20, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long amountIn;

    @Column(length = 1000)
    private String rawContent;

    @Column(length = 50)
    private String gateway;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt = LocalDateTime.now();
}
