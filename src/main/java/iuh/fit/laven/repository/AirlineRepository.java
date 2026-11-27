package iuh.fit.laven.repository;

import iuh.fit.laven.model.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AirlineRepository extends JpaRepository<Airline, String> {
    boolean existsByAirlineNameIgnoreCase(String airlineName);

    boolean existsByAirlineNameIgnoreCaseAndAirlineIdNot(String airlineName, String airlineId);

    List<Airline> findByStatusTrueOrderByAirlineNameAsc();

    /** Tiêu chí null thì bỏ qua, có nhập thì AND; không nhập gì thì trả tất cả. */
    @Query("""
        SELECT a FROM Airline a
        WHERE (:airlineName IS NULL OR LOWER(a.airlineName) LIKE LOWER(CONCAT('%', :airlineName, '%')))
          AND (:status IS NULL OR a.status = :status)
          AND (:verifiedFrom IS NULL OR a.verifiedAt >= :verifiedFrom)
          AND (:verifiedTo IS NULL OR a.verifiedAt <= :verifiedTo)
        ORDER BY a.airlineName ASC
        """)
    List<Airline> search(
        @Param("airlineName") String airlineName,
        @Param("status") Boolean status,
        @Param("verifiedFrom") LocalDate verifiedFrom,
        @Param("verifiedTo") LocalDate verifiedTo
    );
}
