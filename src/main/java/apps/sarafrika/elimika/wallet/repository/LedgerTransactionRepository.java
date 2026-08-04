package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.LedgerTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByUuid(UUID uuid);
}
