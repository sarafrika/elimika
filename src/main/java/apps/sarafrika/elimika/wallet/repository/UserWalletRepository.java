package apps.sarafrika.elimika.wallet.repository;

import apps.sarafrika.elimika.wallet.entity.UserWallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {

    Optional<UserWallet> findByUuid(UUID uuid);

    Optional<UserWallet> findByUserUuidAndCurrencyCode(UUID userUuid, String currencyCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from UserWallet wallet where wallet.userUuid = :userUuid and wallet.currencyCode = :currencyCode")
    Optional<UserWallet> findLockedByUserUuidAndCurrencyCode(
            @Param("userUuid") UUID userUuid,
            @Param("currencyCode") String currencyCode
    );
}
