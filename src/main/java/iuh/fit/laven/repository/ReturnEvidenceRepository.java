package iuh.fit.laven.repository;

import iuh.fit.laven.model.ReturnEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReturnEvidenceRepository extends JpaRepository<ReturnEvidence,String> {

    List<ReturnEvidence> findAllByOrder_OrderIdOrderByReturnEvidenceIdAsc(String orderId);

    List<ReturnEvidence> findAllByOrder_OrderIdInOrderByReturnEvidenceIdAsc(Collection<String> orderIds);
}
