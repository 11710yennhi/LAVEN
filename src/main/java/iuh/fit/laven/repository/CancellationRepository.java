package iuh.fit.laven.repository;

import iuh.fit.laven.model.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRepository extends JpaRepository<Cancellation,String> {
}
