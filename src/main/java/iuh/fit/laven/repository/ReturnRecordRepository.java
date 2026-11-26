package iuh.fit.laven.repository;

import iuh.fit.laven.model.ReturnRecord;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReturnRecordRepository extends JpaRepository<ReturnRecord,String>, JpaSpecificationExecutor<ReturnRecord> {

    boolean existsByOrder_OrderId(String orderOrderId);

    /** UC36: mã đơn của các phiếu trả hàng đang APPROVED, đã duyệt trước mốc deadline, trừ NOT_RECEIVED. */
    @Query("""
        SELECT r.orderId
        FROM ReturnRecord r
        WHERE r.returnType = :returnType
          AND r.status = :status
          AND r.returnReason <> :excludedReason
          AND r.approvedAt < :deadline
        """)
    List<String> findExpiredApprovedOrderIds(
            @Param("returnType") ReturnType returnType,
            @Param("status") ReturnStatus status,
            @Param("excludedReason") ReturnReason excludedReason,
            @Param("deadline") LocalDateTime deadline
    );

    long countByStatus(ReturnStatus status);
}
