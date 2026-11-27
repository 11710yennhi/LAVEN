package iuh.fit.laven.repository;

import iuh.fit.laven.model.ChatUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, String> {

    /** Tạo dòng đếm của ngày nếu chưa có; đã có thì bỏ qua (an toàn khi 2 request cùng tạo). */
    @Transactional
    @Modifying
    @Query(value = """
        INSERT IGNORE INTO chat_usages (usageId, subjectKey, usageDate, questionCount)
        VALUES (:usageId, :subjectKey, :usageDate, 0)
        """, nativeQuery = true)
    void createIfAbsent(@Param("usageId") String usageId,
                        @Param("subjectKey") String subjectKey,
                        @Param("usageDate") LocalDate usageDate);

    /**
     * Cộng 1 lượt nếu chưa chạm giới hạn. Trả 1 = được hỏi, 0 = hết lượt.
     * Kiểm tra và cộng trong cùng một câu UPDATE nên 2 request cùng lúc không vượt giới hạn.
     */
    @Transactional
    @Modifying
    @Query("""
        UPDATE ChatUsage u SET u.questionCount = u.questionCount + 1
        WHERE u.usageId = :usageId AND u.questionCount < :limit
        """)
    int increaseIfUnder(@Param("usageId") String usageId, @Param("limit") int limit);

    /** Hoàn lại 1 lượt khi gọi AI lỗi. */
    @Transactional
    @Modifying
    @Query("""
        UPDATE ChatUsage u SET u.questionCount = u.questionCount - 1
        WHERE u.usageId = :usageId AND u.questionCount > 0
        """)
    int decrease(@Param("usageId") String usageId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatUsage u WHERE u.usageDate < :date")
    int deleteByUsageDateBefore(@Param("date") LocalDate date);
}
