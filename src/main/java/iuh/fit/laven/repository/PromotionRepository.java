package iuh.fit.laven.repository;

import iuh.fit.laven.model.Promotion;
import iuh.fit.laven.model.enums.PromotionApplyType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, String> {

    Optional<Promotion> findByCode(String code);

    /** Khóa bi quan trên Promotion trước khi đếm lượt dùng (tài liệu mục i.7). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.code = :code")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

    List<Promotion> findByApplyTypeAndStatusTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            PromotionApplyType applyType, LocalDate startDate, LocalDate endDate);

    List<Promotion> findByStatusTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate startDate, LocalDate endDate);

    List<Promotion> findByCustomerIsNull();

    List<Promotion> findByCustomerIsNotNull();
}
