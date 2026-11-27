package iuh.fit.laven.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Số câu đã hỏi chatbot trong một ngày.
 * subjectKey: "G:<sha256 IP>" (khách chưa đăng nhập), "U:<userId>" (tài khoản), "ALL" (toàn hệ thống).
 * usageId = subjectKey + "|" + ngày, nên mỗi đối tượng chỉ có một dòng mỗi ngày.
 */
@Entity
@Table(name = "chat_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsage {
    @Id
    @Column(length = 120, nullable = false, updatable = false)
    private String usageId;

    @Column(nullable = false, length = 80)
    private String subjectKey;

    @Column(nullable = false)
    private LocalDate usageDate;

    @Column(nullable = false)
    private int questionCount;
}
