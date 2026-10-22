package apps.sarafrika.elimika.wallet.service;

import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Moves withdrawals along without anyone watching: hands cleared ones to M-Pesa, asks M-Pesa about
 * those in flight, and expires requests whose code was never entered. Each withdrawal is handled on
 * its own, so one failure never stops the rest.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawalProcessor {

    private final WithdrawalRepository withdrawalRepository;
    private final WithdrawalService withdrawalService;

    @Value("${wallet.withdrawals.confirmation-window:PT15M}")
    private Duration confirmationWindow;

    @Scheduled(initialDelayString = "${wallet.withdrawals.process-initial-delay:PT1M}",
            fixedDelayString = "${wallet.withdrawals.process-interval:PT30S}")
    public void process() {
        each(withdrawalRepository.findUuidsByStatus(WithdrawalStatus.SENDING), withdrawalService::send, "send");
        each(withdrawalRepository.findUuidsByStatus(WithdrawalStatus.PROCESSING), withdrawalService::refresh, "refresh");
        each(withdrawalRepository.findUuidsByStatusCreatedBefore(WithdrawalStatus.AWAITING_CONFIRMATION,
                LocalDateTime.now(ZoneOffset.UTC).minus(confirmationWindow)), withdrawalService::expire, "expire");
    }

    private static void each(Iterable<UUID> uuids, Consumer<UUID> step, String name) {
        for (UUID uuid : uuids) {
            try {
                step.accept(uuid);
            } catch (RuntimeException ex) {
                log.error("Withdrawal {} could not {}: {}", uuid, name, ex.getMessage(), ex);
            }
        }
    }
}
