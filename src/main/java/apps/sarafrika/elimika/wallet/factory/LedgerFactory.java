package apps.sarafrika.elimika.wallet.factory;

import apps.sarafrika.elimika.wallet.entity.LedgerAccount;
import apps.sarafrika.elimika.wallet.entity.LedgerEntry;
import apps.sarafrika.elimika.wallet.entity.LedgerTransaction;
import apps.sarafrika.elimika.wallet.enums.LedgerAccountStatus;
import apps.sarafrika.elimika.wallet.ledger.LedgerAccountRef;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingLeg;
import apps.sarafrika.elimika.wallet.ledger.LedgerPostingRequest;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Assembly of ledger rows. The repository convention is factories over builders, so every field a
 * ledger row is created with is visible in one place.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LedgerFactory {

    public static LedgerAccount toAccount(LedgerAccountRef ref) {
        LedgerAccount account = new LedgerAccount();
        account.setOwnerType(ref.ownerType());
        account.setOwnerUuid(ref.ownerUuid());
        account.setAccountType(ref.accountType());
        account.setPurse(ref.purse());
        account.setCurrencyCode(ref.currencyCode());
        account.setStatus(LedgerAccountStatus.ACTIVE);
        return account;
    }

    public static LedgerTransaction toTransaction(LedgerPostingRequest request) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setIdempotencyKey(request.idempotencyKey());
        transaction.setOccurredAt(request.occurredAt());
        transaction.setDescription(request.description());
        transaction.setCauseType(request.causeType());
        transaction.setCauseUuid(request.causeUuid());
        return transaction;
    }

    public static LedgerEntry toEntry(UUID transactionUuid, UUID accountUuid, LedgerPostingLeg leg) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransactionUuid(transactionUuid);
        entry.setAccountUuid(accountUuid);
        entry.setDirection(leg.direction());
        entry.setAmount(leg.amount());
        entry.setCurrencyCode(leg.account().currencyCode());
        return entry;
    }
}
