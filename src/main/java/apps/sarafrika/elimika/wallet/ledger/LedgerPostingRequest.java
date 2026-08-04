package apps.sarafrika.elimika.wallet.ledger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A request to post one balanced transaction.
 *
 * @param idempotencyKey caller-derived and unique; re-posting the same key is a no-op
 * @param causeType      a free label describing what caused the posting. The ledger stores it and
 *                       never interprets it - that is what keeps this module policy-free.
 */
public record LedgerPostingRequest(
        String idempotencyKey,
        LocalDateTime occurredAt,
        String description,
        String causeType,
        UUID causeUuid,
        List<LedgerPostingLeg> legs
) {

    public LedgerPostingRequest {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        Objects.requireNonNull(legs, "legs are required");
        legs = List.copyOf(legs);
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }
}
