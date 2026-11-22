package iuh.fit.laven.repository;

import iuh.fit.laven.model.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, String> {
}
