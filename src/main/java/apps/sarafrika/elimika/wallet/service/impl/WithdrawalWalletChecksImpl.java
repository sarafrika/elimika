package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.wallet.entity.Withdrawal;
import apps.sarafrika.elimika.wallet.enums.WithdrawalStatus;
import apps.sarafrika.elimika.wallet.repository.HeldWithdrawalTotals;
import apps.sarafrika.elimika.wallet.repository.UserWalletRepository;
import apps.sarafrika.elimika.wallet.repository.WalletCheckBalance;
import apps.sarafrika.elimika.wallet.repository.WithdrawalRepository;
import apps.sarafrika.elimika.wallet.service.WalletCheck;
import apps.sarafrika.elimika.wallet.service.WithdrawalWalletChecks;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalWalletChecksImpl implements WithdrawalWalletChecks {

    private static final List<String> HELD = WithdrawalStatus.HELD.stream().map(Enum::name).toList();

    private final UserWalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, WalletCheck> check(Collection<Withdrawal> withdrawals) {
        Map<UUID, WalletCheck> checks = new LinkedHashMap<>();
        if (withdrawals == null || withdrawals.isEmpty()) {
            return checks;
        }
        Set<UUID> owners = withdrawals.stream().map(Withdrawal::getOwnerUuid).collect(Collectors.toSet());
        Map<Key, WalletCheckBalance> wallets = new HashMap<>();
        for (WalletCheckBalance row : walletRepository.findWalletCheckBalances(owners)) {
            wallets.put(new Key(row.getOwnerType(), row.getOwnerUuid(), row.getCurrencyCode()), row);
        }
        Map<Key, HeldWithdrawalTotals> held = new HashMap<>();
        for (HeldWithdrawalTotals row : withdrawalRepository.heldTotalsByOwner(owners, HELD)) {
            held.put(new Key(row.getOwnerType(), row.getOwnerUuid(), row.getCurrencyCode()), row);
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (Withdrawal w : withdrawals) {
            Key key = new Key(w.getOwnerType().name(), w.getOwnerUuid(), w.getCurrencyCode());
            checks.put(w.getUuid(), check(w, wallets.get(key), held.get(key), now));
        }
        return checks;
    }

    private static WalletCheck check(Withdrawal w, WalletCheckBalance wallet, HeldWithdrawalTotals held,
                                     OffsetDateTime now) {
        BigDecimal available = wallet == null ? BigDecimal.ZERO : wallet.getWalletBalance();
        BigDecimal heldAmount = held == null ? BigDecimal.ZERO : held.getHeldAmount();
        BigDecimal heldDebited = held == null ? BigDecimal.ZERO : held.getHeldDebited();
        BigDecimal account = available.add(heldAmount);
        // Only a withdrawal still to be paid has a debit left to come off the account.
        boolean pending = w.getStatus() == WithdrawalStatus.AWAITING_CONFIRMATION
                || WithdrawalStatus.HELD.contains(w.getStatus());
        BigDecimal after = pending ? account.subtract(w.getAmount()) : null;
        BigDecimal ledger = wallet == null ? null : wallet.getLedgerBalance();
        Boolean matches = ledger == null ? null : ledger.compareTo(available) == 0;
        return new WalletCheck(account, heldAmount, held == null ? 0 : held.getHeldCount(), heldDebited, available,
                after, ledger, matches, heldDebited.compareTo(heldAmount) >= 0, now);
    }

    private record Key(String ownerType, UUID ownerUuid, String currencyCode) {
    }
}
