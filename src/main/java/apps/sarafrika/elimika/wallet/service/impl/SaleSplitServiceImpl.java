package apps.sarafrika.elimika.wallet.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.PartyNameLookupService;
import apps.sarafrika.elimika.shared.spi.revenue.SaleDeliveryKind;
import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegSide;
import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitDetail;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitView;
import apps.sarafrika.elimika.wallet.dto.SaleFeeRuleDTO;
import apps.sarafrika.elimika.wallet.dto.SaleSplitDTO;
import apps.sarafrika.elimika.wallet.dto.SaleSplitPartyDTO;
import apps.sarafrika.elimika.wallet.dto.SaleSplitTransactionDTO;
import apps.sarafrika.elimika.wallet.entity.UserWalletTransaction;
import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.repository.UserWalletTransactionRepository;
import apps.sarafrika.elimika.wallet.service.SaleSplitRole;
import apps.sarafrika.elimika.wallet.service.SaleSplitService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleSplitServiceImpl implements SaleSplitService {

    private static final String CREATOR = ":creator";
    private static final String DELIVERY = ":delivery";
    private static final String FEE = ":fee";
    private static final int SCALE = 2;

    private final SaleSplitQueryService splitQueries;
    private final UserWalletTransactionRepository transactionRepository;
    private final ObjectProvider<PartyNameLookupService> partyNames;

    @Override
    public Page<SaleSplitDTO> salesOf(WalletOwner owner, Pageable pageable) {
        return salesOf(owner, null, pageable);
    }

    @Override
    public Page<SaleSplitDTO> salesOf(WalletOwner owner, WalletPurse purse, Pageable pageable) {
        Viewer viewer = Viewer.of(owner, purse);
        if (viewer.paysNoLeg()) {
            return Page.empty(pageable);
        }
        Page<SaleSplitView> page = splitQueries.findPaidTo(partyType(owner), owner.uuid(), viewer.side(), pageable);
        return new PageImpl<>(toDtos(page.getContent(), viewer), page.getPageable(), page.getTotalElements());
    }

    @Override
    public SaleSplitDTO saleOf(WalletOwner owner, UUID splitUuid) {
        return saleOf(owner, null, splitUuid);
    }

    @Override
    public SaleSplitDTO saleOf(WalletOwner owner, WalletPurse purse, UUID splitUuid) {
        Viewer viewer = Viewer.of(owner, purse);
        SaleSplitView split = splitQueries.findByUuid(splitUuid)
                .filter(s -> viewer.creatorLeg(s) || viewer.deliveryLeg(s))
                .orElseThrow(() -> new ResourceNotFoundException("Sale split not found."));
        return toDtos(List.of(split), viewer).get(0);
    }

    @Override
    public List<SaleSplitDTO> splitsOfOrder(String orderId) {
        return toDtos(splitQueries.findByOrderId(orderId), null);
    }

    @Override
    public SaleSplitDTO split(UUID splitUuid) {
        SaleSplitView split = splitQueries.findByUuid(splitUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Sale split not found."));
        return toDtos(List.of(split), null).get(0);
    }

    @Override
    public Map<UUID, UUID> splitUuidsOf(Collection<UserWalletTransaction> transactions) {
        Map<UUID, String> lineByTransaction = new HashMap<>();
        for (UserWalletTransaction txn : transactions) {
            if (isSaleRow(txn)) {
                String line = lineReferenceOf(txn.getReference());
                if (line != null) {
                    lineByTransaction.put(txn.getUuid(), line);
                }
            }
        }
        if (lineByTransaction.isEmpty()) {
            return Map.of();
        }
        Map<String, UUID> splitByLine = splitQueries.findSplitUuidsByLineReference(
                new LinkedHashSet<>(lineByTransaction.values()));
        Map<UUID, UUID> result = new HashMap<>();
        lineByTransaction.forEach((txn, line) -> {
            UUID split = splitByLine.get(line);
            if (split != null) {
                result.put(txn, split);
            }
        });
        return result;
    }

    /** {@code order:line:creator} and {@code order:line:creator:fee} both belong to line {@code order:line}. */
    static String lineReferenceOf(String reference) {
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        String leg = stripFee(reference);
        if (leg.endsWith(CREATOR)) {
            return leg.substring(0, leg.length() - CREATOR.length());
        }
        if (leg.endsWith(DELIVERY)) {
            return leg.substring(0, leg.length() - DELIVERY.length());
        }
        return null;
    }

    private List<SaleSplitDTO> toDtos(List<SaleSplitView> splits, Viewer viewer) {
        if (splits.isEmpty()) {
            return List.of();
        }
        Names names = names(splits);
        Map<String, List<UserWalletTransaction>> rowsByLine = rowsByLine(splits);
        List<SaleSplitDTO> result = new ArrayList<>(splits.size());
        for (SaleSplitView split : splits) {
            result.add(toDto(split, viewer, names, rowsByLine.getOrDefault(split.lineReference(), List.of())));
        }
        return result;
    }

    private SaleSplitDTO toDto(SaleSplitView split, Viewer viewer, Names names, List<UserWalletTransaction> rows) {
        boolean viewerIsCreator = viewer != null && viewer.creatorLeg(split);
        boolean viewerDelivers = viewer != null && viewer.deliveryLeg(split);
        SaleSplitRole deliveryRole = deliveryRole(split.deliveryKind());
        WalletOwner owner = viewer == null ? null : viewer.owner();

        SaleSplitRole yourRole = null;
        WalletPurse yourPurse = null;
        BigDecimal yourGross = null;
        BigDecimal yourFee = null;
        BigDecimal yourNet = null;
        if (viewerIsCreator || viewerDelivers) {
            yourRole = viewerIsCreator ? SaleSplitRole.COURSE_CREATOR : deliveryRole;
            if (!(viewerIsCreator && viewerDelivers)) {
                yourPurse = viewerIsCreator ? WalletPurse.creatorLegOf(owner.type())
                        : WalletPurse.deliveryLegOf(owner.type());
            } else if (owner.type() == WalletOwnerType.ORGANISATION) {
                yourPurse = WalletPurse.EARNINGS;
            }
            yourGross = money(BigDecimal.ZERO);
            yourFee = money(BigDecimal.ZERO);
            yourNet = money(BigDecimal.ZERO);
            for (SaleSplitDetail.Leg leg : viewerIsCreator && viewerDelivers ? List.of(split.creator(), split.delivery())
                    : List.of(viewerIsCreator ? split.creator() : split.delivery())) {
                yourGross = yourGross.add(money(leg.gross()));
                yourFee = yourFee.add(money(leg.fee()));
                yourNet = yourNet.add(money(leg.net()));
            }
        }

        List<SaleSplitTransactionDTO> transactions = rows.stream()
                .filter(row -> viewer == null || viewer.shows(row))
                .map(row -> new SaleSplitTransactionDTO(row.getUuid(), row.getWallet().getUuid(),
                        row.getTransactionType(),
                        stripFee(row.getReference()).endsWith(CREATOR) ? SaleSplitRole.COURSE_CREATOR : deliveryRole,
                        row.getWallet().getPurse(), row.getReference(), money(row.getAmount())))
                .toList();

        return new SaleSplitDTO(
                split.splitUuid(),
                split.orderId(),
                split.orderNumber(),
                split.lineItemId(),
                split.title(),
                split.courseUuid(),
                split.classDefinitionUuid(),
                split.settledAt(),
                split.currencyCode(),
                money(split.lineGross()),
                split.creatorPercentage() == null ? null : split.creatorPercentage().stripTrailingZeros(),
                yourRole,
                yourPurse,
                yourGross,
                yourFee,
                yourNet,
                party(SaleSplitRole.COURSE_CREATOR, split.creator(), owner, names),
                party(deliveryRole, split.delivery(), owner, names),
                money(split.platformFeeTotal()),
                money(split.retained()),
                split.reconciles(),
                split.backfilled(),
                feeRule(split),
                transactions);
    }

    private static SaleSplitPartyDTO party(SaleSplitRole role, SaleSplitDetail.Leg leg, WalletOwner viewer, Names names) {
        WalletOwner owner = ownerOf(leg);
        boolean showUuid = owner != null && (viewer == null || viewer.equals(owner));
        return new SaleSplitPartyDTO(role, names.of(owner), owner == null ? null : owner.type(),
                showUuid ? owner.uuid() : null, leg.status(), money(leg.gross()), money(leg.fee()), money(leg.net()));
    }

    static SaleFeeRuleDTO feeRule(SaleSplitView split) {
        SaleSplitDetail.FeeRule rule = split.feeRule();
        if (rule == null) {
            String description = split.backfilled()
                    ? "Not recorded for lines settled before splits were kept"
                    : "No platform fee";
            return new SaleFeeRuleDTO(description, !split.backfilled(), null, null, null, null, null, null, null,
                    null, null);
        }
        String description;
        if (rule.mode() == null || rule.rate() == null) {
            description = "No platform fee";
        } else {
            String base = rule.mode() == SaleFeeMode.PERCENTAGE
                    ? plain(rule.rate()) + "% of each share"
                    : amount(rule.rateCurrency() == null ? split.currencyCode() : rule.rateCurrency(), rule.rate())
                    + " on each share";
            if (Boolean.TRUE.equals(rule.waived())) {
                description = "Waived (normally " + base + ")";
            } else if (rule.discountPercentage() != null) {
                description = base + ", less a " + plain(rule.discountPercentage()) + "% discount";
            } else if (rule.discountAmount() != null) {
                description = base + ", less " + amount(split.currencyCode(), rule.discountAmount());
            } else {
                description = base;
            }
        }
        return new SaleFeeRuleDTO(description, true, rule.ruleUuid(), rule.ruleKey(), rule.mode(),
                rule.rate() == null ? null : rule.rate().stripTrailingZeros(), rule.rateCurrency(), rule.waived(),
                rule.discountPercentage(), rule.discountAmount(), rule.evaluatedAt());
    }

    private static SaleSplitRole deliveryRole(SaleDeliveryKind kind) {
        if (kind == SaleDeliveryKind.ORGANISATION) {
            return SaleSplitRole.ORGANISATION;
        }
        return kind == SaleDeliveryKind.INSTRUCTOR ? SaleSplitRole.INSTRUCTOR : null;
    }

    private static boolean paidTo(SaleSplitDetail.Leg leg, WalletOwner owner) {
        return leg.paid() && owner.equals(ownerOf(leg));
    }

    private static WalletOwner ownerOf(SaleSplitDetail.Leg leg) {
        if (leg.ownerType() == null || leg.ownerUuid() == null) {
            return null;
        }
        return new WalletOwner(WalletOwnerType.valueOf(leg.ownerType().name()), leg.ownerUuid());
    }

    /**
     * Who is looking, and through which purse: a person's COURSE_CREATOR purse sees only their creator legs,
     * INSTRUCTOR only their delivery legs, STUDENT none; no purse (or an organisation's EARNINGS) sees both.
     */
    private record Viewer(WalletOwner owner, WalletPurse purse) {

        static Viewer of(WalletOwner owner, WalletPurse purse) {
            if (purse != null && !purse.isAllowedFor(owner.type())) {
                throw new IllegalArgumentException("A " + owner.type().name().toLowerCase(java.util.Locale.ROOT)
                        + " wallet has no " + purse + " purse");
            }
            return new Viewer(owner, purse == WalletPurse.EARNINGS ? null : purse);
        }

        boolean paysNoLeg() {
            return purse == WalletPurse.STUDENT;
        }

        SaleLegSide side() {
            if (purse == WalletPurse.COURSE_CREATOR) {
                return SaleLegSide.CREATOR;
            }
            return purse == WalletPurse.INSTRUCTOR ? SaleLegSide.DELIVERY : null;
        }

        boolean creatorLeg(SaleSplitView split) {
            return (purse == null || purse == WalletPurse.COURSE_CREATOR) && paidTo(split.creator(), owner);
        }

        boolean deliveryLeg(SaleSplitView split) {
            return (purse == null || purse == WalletPurse.INSTRUCTOR) && paidTo(split.delivery(), owner);
        }

        boolean shows(UserWalletTransaction row) {
            return owner.equals(row.getWallet().owner()) && (purse == null || row.getWallet().getPurse() == purse);
        }
    }

    private static SalePartyType partyType(WalletOwner owner) {
        return SalePartyType.valueOf(owner.type().name());
    }

    private static boolean isSaleRow(UserWalletTransaction txn) {
        return txn.getTransactionType() == WalletTransactionType.SALE
                || txn.getTransactionType() == WalletTransactionType.PLATFORM_FEE;
    }

    private static String stripFee(String reference) {
        return reference.endsWith(FEE) ? reference.substring(0, reference.length() - FEE.length()) : reference;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String amount(String currency, BigDecimal value) {
        String figure = value.setScale(SCALE, RoundingMode.HALF_UP).toPlainString();
        return currency == null ? figure : currency + " " + figure;
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private Map<String, List<UserWalletTransaction>> rowsByLine(List<SaleSplitView> splits) {
        Set<String> references = new LinkedHashSet<>();
        for (SaleSplitView split : splits) {
            String line = split.lineReference();
            references.addAll(List.of(line + CREATOR, line + CREATOR + FEE, line + DELIVERY, line + DELIVERY + FEE));
        }
        Map<String, List<UserWalletTransaction>> byLine = new HashMap<>();
        for (UserWalletTransaction row : transactionRepository.findByReferenceInWithWallet(references)) {
            if (isSaleRow(row)) {
                byLine.computeIfAbsent(lineReferenceOf(row.getReference()), key -> new ArrayList<>()).add(row);
            }
        }
        return byLine;
    }

    private Names names(List<SaleSplitView> splits) {
        PartyNameLookupService lookup = partyNames.getIfAvailable();
        if (lookup == null) {
            return new Names(Map.of(), Map.of());
        }
        Set<UUID> users = new LinkedHashSet<>();
        Set<UUID> organisations = new LinkedHashSet<>();
        splits.stream()
                .flatMap(split -> Stream.of(ownerOf(split.creator()), ownerOf(split.delivery())))
                .filter(Objects::nonNull)
                .forEach(owner -> (owner.type() == WalletOwnerType.ORGANISATION ? organisations : users).add(owner.uuid()));
        return new Names(users.isEmpty() ? Map.of() : lookup.findUserNames(users),
                organisations.isEmpty() ? Map.of() : lookup.findOrganisationNames(organisations));
    }

    private record Names(Map<UUID, String> users, Map<UUID, String> organisations) {
        String of(WalletOwner owner) {
            if (owner == null) {
                return null;
            }
            return owner.type() == WalletOwnerType.ORGANISATION ? organisations.get(owner.uuid()) : users.get(owner.uuid());
        }
    }
}
