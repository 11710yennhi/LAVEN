package iuh.fit.laven.repository;

import iuh.fit.laven.model.BaggageRule;
import iuh.fit.laven.model.enums.BaggageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BaggageRuleRepository extends JpaRepository<BaggageRule, String> {
    List<BaggageRule> findByAirlineAirlineIdOrderByBaggageTypeAscFareClassAsc(String airlineId);

    /** Quy định của mọi hãng đang dùng theo loại hành lý (tool gợi ý khi khách không nói hãng). */
    List<BaggageRule> findByAirlineStatusTrueAndBaggageType(BaggageType baggageType);

    boolean existsByAirlineAirlineIdAndBaggageTypeAndFareClassIgnoreCase(
        String airlineId, BaggageType baggageType, String fareClass);

    boolean existsByAirlineAirlineIdAndBaggageTypeAndFareClassIgnoreCaseAndBaggageRuleIdNot(
        String airlineId, BaggageType baggageType, String fareClass, String baggageRuleId);

    /** Tiêu chí null thì bỏ qua, có nhập thì AND; không nhập gì thì trả tất cả. */
    @Query("""
        SELECT r FROM BaggageRule r
        WHERE (:airlineId IS NULL OR r.airline.airlineId = :airlineId)
          AND (:airlineName IS NULL OR LOWER(r.airline.airlineName) LIKE LOWER(CONCAT('%', :airlineName, '%')))
          AND (:baggageType IS NULL OR r.baggageType = :baggageType)
          AND (:fareClass IS NULL OR LOWER(r.fareClass) LIKE LOWER(CONCAT('%', :fareClass, '%')))
          AND (:airlineStatus IS NULL OR r.airline.status = :airlineStatus)
          AND (:verifiedFrom IS NULL OR r.airline.verifiedAt >= :verifiedFrom)
          AND (:verifiedTo IS NULL OR r.airline.verifiedAt <= :verifiedTo)
        ORDER BY r.airline.airlineName ASC, r.baggageType ASC, r.fareClass ASC
        """)
    List<BaggageRule> search(
        @Param("airlineId") String airlineId,
        @Param("airlineName") String airlineName,
        @Param("baggageType") BaggageType baggageType,
        @Param("fareClass") String fareClass,
        @Param("airlineStatus") Boolean airlineStatus,
        @Param("verifiedFrom") LocalDate verifiedFrom,
        @Param("verifiedTo") LocalDate verifiedTo
    );
}
