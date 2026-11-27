package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // Cột orderId trong bank_transactions là khóa ngoại đến orders.orderId.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orderId", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long amountIn;

    // Số tài khoản nhận tiền và thời gian giao dịch lấy từ JSON webhook của SePay.
    @Column(length = 50, nullable = false)
    private String accountNumber;

    @Column(length = 1000)
    private String rawContent;

    @Column(length = 50)
    private String gateway;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // Thời điểm server LAVEN xử lý giao dịch, khác transactionDate của ngân hàng.
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt = LocalDateTime.now();
}
