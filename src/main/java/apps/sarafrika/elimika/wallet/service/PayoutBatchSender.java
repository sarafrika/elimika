package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.enums.PayoutBatchStatus;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.PayoutBatchRepository;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hands the rows of released payout batches to M-Pesa, oldest batch first, keeping at most
 * {@code max-in-flight} batch rows with M-Pesa at once, and closes each batch when its rows finish.
 * Each row is sent under its withdrawal UUID as the client reference, so a restart or a second run
 * finds the payout already started instead of paying twice.
 */
@Component
@Slf4j
public class PayoutBatchSender {

    private final WithdrawalRepository withdrawalRepository;
    private final PayoutBatchRepository batchRepository;
    private final WithdrawalService withdrawalService;
    private final PayoutBatchService batchService;
    private final int maxInFlight;
    private final int perTick;

    public PayoutBatchSender(WithdrawalRepository withdrawalRepository, PayoutBatchRepository batchRepository,
                             WithdrawalService withdrawalService, PayoutBatchService batchService,
                             @Value("${wallet.payout.batch.max-in-flight:5}") int maxInFlight,
                             @Value("${wallet.payout.batch.per-tick:10}") int perTick) {
        this.withdrawalRepository = withdrawalRepository;
        this.batchRepository = batchRepository;
        this.withdrawalService = withdrawalService;
        this.batchService = batchService;
        this.maxInFlight = maxInFlight;
        this.perTick = perTick;
    }

    @Scheduled(initialDelayString = "${wallet.payout.batch.send-initial-delay:PT1M}",
            fixedDelayString = "${wallet.payout.batch.send-interval:PT15S}")
    public void tick() {
        long inFlight = withdrawalRepository.countByStatusAndPayoutBatchUuidIsNotNull(WithdrawalStatus.PROCESSING);
        int slots = (int) Math.min(perTick, maxInFlight - inFlight);
        if (slots > 0) {
            List<UUID> due = withdrawalRepository.findUuidsInBatchesByStatus(PayoutBatchStatus.RELEASED,
                    WithdrawalStatus.SENDING, PageRequest.of(0, slots));
            for (UUID uuid : due) {
                try {
                    withdrawalService.send(uuid);
                } catch (RuntimeException ex) {
                    log.error("Batch withdrawal {} could not be sent: {}", uuid, ex.getMessage(), ex);
                }
            }
        }
        for (UUID batchUuid : batchRepository.findUuidsByStatus(PayoutBatchStatus.RELEASED)) {
            try {
                batchService.settle(batchUuid);
            } catch (RuntimeException ex) {
                log.error("Payout batch {} could not be settled: {}", batchUuid, ex.getMessage(), ex);
            }
        }
    }
}
