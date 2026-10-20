package apps.sarafrika.elimika.payout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
@DisplayName("A captured class sale pays the creator and the delivery party, each net of their own platform fee")
class OrderCaptureWalletCreditListenerTest {

    private static final String ORDER_ID = "11111111-1111-1111-1111-111111111111";
    private static final OffsetDateTime ORDERED_AT = OffsetDateTime.of(2026, 10, 20, 9, 0, 0, 0, ZoneOffset.UTC);
    private static final PlatformFeeConfig TEN_PERCENT =
            new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), "KES", null, null);

    @Mock
    private WalletService walletService;
    @Mock
    private CourseInfoService courseInfoService;
    @Mock
    private InstructorLookupService instructorLookupService;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private PurchaseSettlementRecorder purchaseSettlementRecorder;
    @Mock
    private RuleEvaluationService ruleEvaluationService;

    @InjectMocks
    private OrderCaptureWalletCreditListener listener;

    private final UUID classUuid = UUID.randomUUID();
    private final UUID courseUuid = UUID.randomUUID();
    private final UUID creatorUserUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();
    private final UUID instructorUserUuid = UUID.randomUUID();
    private final UUID organisationUuid = UUID.randomUUID();

    @Test
    @DisplayName("worked example, instructor's own class: creator 13,500 and instructor 31,500 after 10% fees")
    void directClassPaysCreatorAndInstructor() {
        givenFee(TEN_PERCENT);
        givenClassOfCourse("Flower Gardening", "30.00");
        givenCreator();
        givenDirectInstructor();
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(true);

        capture(classLine("line-1", "50000.00"));

        verify(walletService).settleSaleShare(eq(WalletOwner.user(creatorUserUuid)), money("15000.00"), money("1500.00"),
                eq("KES"), eq(ORDER_ID + ":line-1:creator"), argThat(d -> d.contains("Flower Gardening")));
        verify(walletService).settleSaleShare(eq(WalletOwner.user(instructorUserUuid)), money("35000.00"),
                money("3500.00"), eq("KES"), eq(ORDER_ID + ":line-1:delivery"), any());
        assertSettlement(settlement(), "50000.00", "5000.00", "45000.00", "0.00");
    }

    @Test
    @DisplayName("worked example, hired through a job: the organisation receives the delivery share, not the instructor")
    void jobClassPaysTheOrganisation() {
        givenFee(TEN_PERCENT);
        givenClassOfCourse("Flower Gardening", "30.00");
        givenCreator();
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.of(organisationUuid));
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(true);

        capture(classLine("line-1", "50000.00"));

        verify(walletService).settleSaleShare(eq(WalletOwner.organisation(organisationUuid)), money("35000.00"),
                money("3500.00"), eq("KES"), eq(ORDER_ID + ":line-1:delivery"), any());
        verify(walletService).settleSaleShare(eq(WalletOwner.user(creatorUserUuid)), money("15000.00"),
                money("1500.00"), any(), any(), any());
        verifyNoInteractions(instructorLookupService);
        assertSettlement(settlement(), "50000.00", "5000.00", "45000.00", "0.00");
    }

    @Test
    @DisplayName("with no fee rule each earner receives their whole share")
    void noFeeRule() {
        givenFee(null);
        givenClassOfCourse("Gaming", "60.00");
        givenCreator();
        givenDirectInstructor();
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(true);

        capture(classLine("line-1", "10000.00"));

        verify(walletService).settleSaleShare(eq(WalletOwner.user(creatorUserUuid)), money("6000.00"), money("0.00"),
                any(), any(), any());
        verify(walletService).settleSaleShare(eq(WalletOwner.user(instructorUserUuid)), money("4000.00"),
                money("0.00"), any(), any(), any());
        assertSettlement(settlement(), "10000.00", "0.00", "10000.00", "0.00");
    }

    @Test
    @DisplayName("the fee rule is read as it stood when the order was placed")
    void feeRuleIsEvaluatedAtOrderTime() {
        givenFee(null);
        givenClassOfCourse("Gaming", "60.00");
        givenCreator();
        givenDirectInstructor();

        capture(classLine("line-1", "100.00"));

        verify(ruleEvaluationService).resolvePlatformFee(argThat(c -> ORDERED_AT.equals(c.evaluationInstant())));
    }

    @Test
    @DisplayName("an unresolvable creator is retained, and no fee is charged on what nobody received")
    void missingCreatorIsRetained() {
        givenFee(TEN_PERCENT);
        givenClassOfCourse("Flower Gardening", "30.00");
        when(courseInfoService.getCourseCreatorUserUuid(courseUuid)).thenReturn(Optional.empty());
        givenDirectInstructor();
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(true);

        capture(classLine("line-1", "50000.00"));

        verify(walletService).settleSaleShare(eq(WalletOwner.user(instructorUserUuid)), any(), any(), any(), any(), any());
        assertSettlement(settlement(), "50000.00", "3500.00", "31500.00", "15000.00");
    }

    @Test
    @DisplayName("a class with no default instructor retains the delivery share")
    void missingInstructorIsRetained() {
        givenFee(TEN_PERCENT);
        givenClassOfCourse("Flower Gardening", "30.00");
        givenCreator();
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.empty());
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid)).thenReturn(Optional.empty());
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(true);

        capture(classLine("line-1", "50000.00"));

        assertSettlement(settlement(), "50000.00", "1500.00", "13500.00", "35000.00");
    }

    @Test
    @DisplayName("a course with no revenue split credits nobody and retains the line")
    void noRevenueShare() {
        givenFee(TEN_PERCENT);
        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(snapshot("Gaming", courseUuid)));
        when(courseInfoService.getRevenueShare(courseUuid)).thenReturn(Optional.empty());

        capture(classLine("line-1", "1000.00"));

        verify(walletService, never()).settleSaleShare(any(), any(), any(), any(), any(), any());
        assertSettlement(settlement(), "1000.00", "0.00", "0.00", "1000.00");
    }

    @Test
    @DisplayName("a line without a class or course is retained whole")
    void unscopedLine() {
        givenFee(TEN_PERCENT);

        capture(new CartItemResponse[]{line("line-1", "1000.00", Map.of())});

        verify(walletService, never()).settleSaleShare(any(), any(), any(), any(), any(), any());
        assertSettlement(settlement(), "1000.00", "0.00", "0.00", "1000.00");
    }

    @Test
    @DisplayName("a free line is neither settled nor recorded")
    void freeLine() {
        givenFee(TEN_PERCENT);

        capture(classLine("line-1", "0.00"));

        verifyNoInteractions(walletService, purchaseSettlementRecorder);
    }

    @Test
    @DisplayName("an order that is not captured pays nobody")
    void notCaptured() {
        listener.handleOrderCompleted(new OrderCompletedEvent(order("AWAITING_PAYMENT", classLine("line-1", "100.00")), null));

        verifyNoInteractions(walletService, purchaseSettlementRecorder, ruleEvaluationService);
    }

    @Test
    @DisplayName("a replayed order records the same settlement without paying twice")
    void replayIsRecordedAgain() {
        givenFee(TEN_PERCENT);
        givenClassOfCourse("Flower Gardening", "30.00");
        givenCreator();
        givenDirectInstructor();
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), any())).thenReturn(false);

        capture(classLine("line-1", "50000.00"));

        assertSettlement(settlement(), "50000.00", "5000.00", "45000.00", "0.00");
    }

    @Test
    @DisplayName("one failing line does not stop the others, and the failure is rethrown for a retry")
    void failureIsIsolatedAndRethrown() {
        givenFee(null);
        givenClassOfCourse("Flower Gardening", "30.00");
        givenCreator();
        givenDirectInstructor();
        when(walletService.settleSaleShare(any(), any(), any(), any(), any(), argThat(d -> d != null)))
                .thenReturn(true);
        when(walletService.settleSaleShare(any(), any(), any(), any(), eq(ORDER_ID + ":line-1:creator"), any()))
                .thenThrow(new IllegalStateException("wallet unavailable"));

        assertThatThrownBy(() -> capture(classLine("line-1", "100.00"), classLine("line-2", "100.00")))
                .isInstanceOf(WalletCreditFailedException.class);

        verify(walletService).settleSaleShare(any(), any(), any(), any(), eq(ORDER_ID + ":line-2:creator"), any());
        assertThat(settlements()).extracting(PurchaseLineSettlement::lineItemId).containsExactly("line-2");
    }

    @Test
    void listenerIsWiredForPersistentRetryableDelivery() throws Exception {
        Method method = OrderCaptureWalletCreditListener.class.getMethod("handleOrderCompleted", OrderCompletedEvent.class);

        TransactionalEventListener eventListener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(eventListener).isNotNull();
        assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(eventListener.fallbackExecution()).isTrue();
        assertThat(method.getAnnotation(Async.class)).isNotNull();
        assertThat(method.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    private void givenFee(PlatformFeeConfig config) {
        when(ruleEvaluationService.resolvePlatformFee(any())).thenReturn(Optional.ofNullable(config));
    }

    private void givenClassOfCourse(String title, String creatorPercentage) {
        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(snapshot(title, courseUuid)));
        BigDecimal creator = new BigDecimal(creatorPercentage);
        when(courseInfoService.getRevenueShare(courseUuid))
                .thenReturn(Optional.of(new RevenueShare(creator, BigDecimal.valueOf(100).subtract(creator))));
    }

    private void givenCreator() {
        when(courseInfoService.getCourseCreatorUserUuid(courseUuid)).thenReturn(Optional.of(creatorUserUuid));
    }

    private void givenDirectInstructor() {
        when(classDefinitionLookupService.findHiringOrganisationUuid(classUuid)).thenReturn(Optional.empty());
        when(classDefinitionLookupService.findDefaultInstructorUuid(classUuid)).thenReturn(Optional.of(instructorUuid));
        when(instructorLookupService.getInstructorUserUuid(instructorUuid)).thenReturn(Optional.of(instructorUserUuid));
    }

    private ClassDefinitionSnapshot snapshot(String title, UUID course) {
        return new ClassDefinitionSnapshot(classUuid, course, null, title, null, null, null, RateBasis.PER_HOUR,
                null, null, null, null, null, null, null);
    }

    private void capture(CartItemResponse... items) {
        listener.handleOrderCompleted(new OrderCompletedEvent(order("CAPTURED", items), null));
    }

    private OrderResponse order(String status, CartItemResponse... items) {
        BigDecimal total = List.of(items).stream().map(CartItemResponse::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return OrderResponse.builder()
                .id(ORDER_ID)
                .paymentStatus(status)
                .currencyCode("KES")
                .total(total)
                .createdAt(ORDERED_AT)
                .items(List.of(items))
                .build();
    }

    private CartItemResponse classLine(String id, String total) {
        return line(id, total, Map.of("class_definition_uuid", classUuid.toString(), "course_uuid", courseUuid.toString()));
    }

    private static CartItemResponse line(String id, String total, Map<String, Object> metadata) {
        return CartItemResponse.builder().id(id).total(new BigDecimal(total)).metadata(metadata).build();
    }

    private List<PurchaseLineSettlement> settlements() {
        ArgumentCaptor<PurchaseLineSettlement> captor = ArgumentCaptor.forClass(PurchaseLineSettlement.class);
        verify(purchaseSettlementRecorder, org.mockito.Mockito.atLeast(0)).recordLineSettlement(captor.capture());
        return captor.getAllValues();
    }

    private PurchaseLineSettlement settlement() {
        assertThat(settlements()).hasSize(1);
        return settlements().getFirst();
    }

    private static BigDecimal money(String value) {
        return argThat(actual -> actual != null && actual.compareTo(new BigDecimal(value)) == 0);
    }

    private static void assertSettlement(PurchaseLineSettlement s, String gross, String fee, String credited, String retained) {
        assertThat(s.grossAmount()).as("gross").isEqualByComparingTo(gross);
        assertThat(s.platformFeeAmount()).as("fee").isEqualByComparingTo(fee);
        assertThat(s.creditedAmount()).as("credited").isEqualByComparingTo(credited);
        assertThat(s.retainedAmount()).as("retained").isEqualByComparingTo(retained);
    }
}
