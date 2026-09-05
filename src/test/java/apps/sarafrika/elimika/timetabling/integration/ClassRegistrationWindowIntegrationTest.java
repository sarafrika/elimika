package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.spi.ClassDefinitionLookupServiceImpl;
import apps.sarafrika.elimika.commerce.catalogue.repository.CommerceCatalogueItemRepository;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCart;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceCartItem;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProduct;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceProductVariant;
import apps.sarafrika.elimika.commerce.internal.enums.CartStatus;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceCartRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderItemRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceProductVariantRepository;
import apps.sarafrika.elimika.commerce.internal.service.RegionResolver;
import apps.sarafrika.elimika.commerce.internal.service.impl.InternalCartServiceImpl;
import apps.sarafrika.elimika.commerce.spi.paywall.CommercePaywallService;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.LearnerProgressLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingService;
import apps.sarafrika.elimika.shared.currency.service.CurrencyValidator;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.service.AgeVerificationService;
import apps.sarafrika.elimika.shared.spi.ClassCapacityService;
import apps.sarafrika.elimika.shared.spi.ClassScheduleService;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationAffiliationService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.security.TimetableSecurityService;
import apps.sarafrika.elimika.timetabling.service.impl.TimetableServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentEligibilityDTO;
import apps.sarafrika.elimika.timetabling.spi.ClassEnrolmentGateServiceImpl;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves the class registration window is a rule and not a label.
 * <p>
 * The rule lives in one method, {@code TimetableServiceImpl.findRegistrationWindowBlocker}, and every
 * way into a class asks it: the buying paths through {@code ClassEnrolmentGateService}, and the
 * enrolment calls directly. Asserting only against the eligibility report would leave both
 * interesting claims untested — that a buyer standing at the checkout with a closed class in their
 * cart is stopped, and that calling the enrolment endpoint instead of buying does not walk straight
 * past the window. So the refusals here are read from {@code InternalCartServiceImpl.completeCart},
 * the method the checkout endpoint calls, and from {@code enrollStudent}, the method
 * {@code POST /api/v1/enrollments} calls — running against the real class definition, the real
 * scheduled session, and a real database built by the real migrations. Only the commerce persistence
 * around them is stubbed.
 * <p>
 * A window that is merely stored, or checked on the buying path alone, would pass nothing below.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(ClassRegistrationWindowIntegrationTest.TestConfig.class)
@DisplayName("A class only accepts enrolments inside its registration window")
class ClassRegistrationWindowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        // The real migrations build the schema; Hibernate must not touch it.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @Autowired
    private ClassDefinitionRepository classDefinitionRepository;
    @Autowired
    private ScheduledInstanceRepository scheduledInstanceRepository;

    /** Held so a test can speak as staff; unstubbed it answers false to everything, i.e. a learner. */
    private DomainSecurityService timetableSecurity;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);
    private static final String CART_ID = "11111111-1111-1111-1111-111111111111";

    private final UUID buyerUserUuid = UUID.randomUUID();
    private final UUID studentUuid = UUID.randomUUID();
    private final UUID instructorUuid = UUID.randomUUID();

    private TimetableServiceImpl timetableService;
    private ClassEnrolmentGateServiceImpl enrolmentGate;
    private InternalCartServiceImpl cartService;

    private CommerceCartRepository cartRepository;
    private CommerceOrderRepository orderRepository;
    private ClassCapacityService classCapacityService;
    private InternalCommerceMapper commerceMapper;

    @BeforeEach
    void wireTheRealGateIntoTheRealCheckout() {
        StudentLookupService studentLookupService = mock(StudentLookupService.class);
        when(studentLookupService.findStudentUuidByUserUuid(buyerUserUuid)).thenReturn(Optional.of(studentUuid));

        // No age limits on the course, so nothing but the window and the seat can refuse this buyer.
        CourseInfoService courseInfoService = mock(CourseInfoService.class);

        timetableSecurity = mock(DomainSecurityService.class);

        timetableService = new TimetableServiceImpl(
                scheduledInstanceRepository,
                enrollmentRepository,
                mock(ApplicationEventPublisher.class),
                mock(GenericSpecificationBuilder.class),
                mock(GenericSpecificationBuilder.class),
                new ClassDefinitionLookupServiceImpl(classDefinitionRepository),
                courseInfoService,
                mock(LearnerProgressLookupService.class),
                mock(AgeVerificationService.class),
                mock(CommercePaywallService.class),
                mock(AvailabilityService.class),
                studentLookupService,
                mock(UserLookupService.class),
                mock(OrganisationLookupService.class),
                mock(OrganisationAffiliationService.class),
                mock(InstructorLookupService.class),
                mock(ResourceBookingService.class),
                timetableSecurity,
                mock(TimetableSecurityService.class));

        enrolmentGate = new ClassEnrolmentGateServiceImpl(timetableService, studentLookupService);

        cartRepository = mock(CommerceCartRepository.class);
        orderRepository = mock(CommerceOrderRepository.class);
        classCapacityService = mock(ClassCapacityService.class);
        commerceMapper = mock(InternalCommerceMapper.class);
        DomainSecurityService cartSecurity = mock(DomainSecurityService.class);
        when(cartSecurity.getCurrentUserUuid()).thenReturn(buyerUserUuid);

        cartService = new InternalCartServiceImpl(
                cartRepository,
                mock(CommerceCartItemRepository.class),
                mock(CommerceProductVariantRepository.class),
                mock(CommerceCatalogueItemRepository.class),
                orderRepository,
                mock(CommerceOrderItemRepository.class),
                commerceMapper,
                new ObjectMapper(),
                mock(RegionResolver.class),
                classCapacityService,
                enrolmentGate,
                mock(ClassScheduleService.class),
                mock(CurrencyValidator.class),
                cartSecurity);
    }

    @Test
    @DisplayName("checkout is refused before the window opens, and names the day it does")
    void checkoutIsRefusedBeforeRegistrationOpens() {
        UUID classUuid = classOpenBetween(TODAY.plusDays(3), TODAY.plusDays(30));
        stageCheckoutFor(classUuid);

        assertThatThrownBy(() -> cartService.completeCart(CART_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("opens on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .as("nothing may be reserved for a class that is not yet taking enrolments")
                .isEmpty();
    }

    @Test
    @DisplayName("checkout is refused after the window closes, and names the day it did")
    void checkoutIsRefusedAfterRegistrationCloses() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        stageCheckoutFor(classUuid);

        assertThatThrownBy(() -> cartService.completeCart(CART_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .as("a closed class must not take a seat hold either")
                .isEmpty();
    }

    @Test
    @DisplayName("checkout completes inside the window, and the seat is held")
    void checkoutSucceedsInsideTheWindow() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(5), TODAY.plusDays(5));
        stageCheckoutFor(classUuid);

        cartService.completeCart(CART_ID);

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .as("an open window lets the checkout through to the seat hold")
                .extracting(Enrollment::getStatus)
                .containsExactly(EnrollmentStatus.RESERVED);
    }

    @Test
    @DisplayName("both ends of the window are open days")
    void theFirstAndLastDayBothCount() {
        UUID opensToday = classOpenBetween(TODAY, TODAY.plusDays(30));
        UUID closesToday = classOpenBetween(TODAY.minusDays(30), TODAY);

        assertThat(enrolmentGate.findEnrolmentBlocker(opensToday, buyerUserUuid))
                .as("the day registration opens is a day it is open")
                .isEmpty();
        assertThat(enrolmentGate.findEnrolmentBlocker(closesToday, buyerUserUuid))
                .as("the day registration closes is still a day it is open")
                .isEmpty();
    }

    @Test
    @DisplayName("the enrolment endpoint refuses a class whose window has closed")
    void enrollingDirectlyIsRefusedAfterTheWindowCloses() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));

        assertThatThrownBy(() -> timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid)))
                .as("POST /api/v1/enrollments takes no cart with it, so the window has to be judged here too")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid)).isEmpty();
    }

    @Test
    @DisplayName("the enrolment endpoint refuses a class whose window has not opened")
    void enrollingDirectlyIsRefusedBeforeTheWindowOpens() {
        UUID classUuid = classOpenBetween(TODAY.plusDays(3), TODAY.plusDays(30));

        assertThatThrownBy(() -> timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("opens on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid)).isEmpty();
    }

    @Test
    @DisplayName("staff running the class may enrol a late learner after the window closes")
    void staffMayEnrolAfterTheWindowCloses() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        // An instructor who runs this class, acting for the learner rather than as them.
        when(timetableSecurity.isStudentWithUuid(studentUuid)).thenReturn(false);
        when(timetableSecurity.canManageClass(classUuid)).thenReturn(true);

        timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid));

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .as("the window informs the person running the class; it does not overrule them")
                .extracting(Enrollment::getStatus)
                .containsExactly(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("a learner who also manages some other class is still bound by the window")
    void selfEnrollingIsBoundEvenForStaff() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        // The caller IS the learner. Whatever else they hold, this is self-service.
        when(timetableSecurity.isStudentWithUuid(studentUuid)).thenReturn(true);
        when(timetableSecurity.canManageClass(classUuid)).thenReturn(true);

        assertThatThrownBy(() -> timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid)))
                .as("the override is for acting on a learner's behalf, never for one's own seat")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid)).isEmpty();
    }

    @Test
    @DisplayName("the enrolment endpoint lets a student in while the window is open")
    void enrollingDirectlyWorksInsideTheWindow() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(5), TODAY.plusDays(5));

        timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid));

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .extracting(Enrollment::getStatus)
                .containsExactly(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("a seat already held at checkout is still converted after the window closes")
    void aLiveSeatHoldSurvivesTheWindowClosing() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        holdASeatFor(classUuid, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

        timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid));

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid))
                .as("the hold was granted while the window was open and the buyer has very likely paid; "
                        + "stranding that seat is worse than an enrolment a minute past closing")
                .extracting(Enrollment::getStatus)
                .containsExactly(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("a hold that has already lapsed does not reopen a closed class")
    void aLapsedSeatHoldDoesNotReopenTheWindow() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        holdASeatFor(classUuid, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5));

        assertThatThrownBy(() -> timetableService.enrollStudent(new EnrollmentRequestDTO(classUuid, studentUuid)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed on");
    }

    @Test
    @DisplayName("enrolling into a single session is refused after the window closes too")
    void enrollingIntoOneInstanceIsRefusedAfterTheWindowCloses() {
        UUID classUuid = classOpenBetween(TODAY.minusDays(30), TODAY.minusDays(1));
        UUID instanceUuid = scheduledInstanceRepository.findByClassDefinitionUuid(classUuid).get(0).getUuid();

        assertThatThrownBy(() -> timetableService.enrollStudentInInstance(instanceUuid, studentUuid))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed on");

        assertThat(enrollmentRepository.findByStudentUuid(studentUuid)).isEmpty();
    }

    @Test
    @DisplayName("eligibility reports the window it judged the learner against")
    void eligibilityPublishesTheWindow() {
        LocalDate opensOn = TODAY.plusDays(2);
        LocalDate closesOn = TODAY.plusDays(20);
        UUID classUuid = classOpenBetween(opensOn, closesOn);

        ClassEnrolmentEligibilityDTO eligibility =
                timetableService.getClassEnrolmentEligibility(classUuid, studentUuid);

        assertThat(eligibility.eligible()).isFalse();
        assertThat(eligibility.registrationOpen()).isFalse();
        assertThat(eligibility.registrationWindow())
                .isEqualTo(new ClassEnrolmentEligibilityDTO.RegistrationWindow(opensOn, closesOn));
        // The window is published on the class listing, so the refusal may name the day it opens.
        assertThat(eligibility.reason())
                .startsWith("Registration for this class opens on")
                .contains(String.valueOf(opensOn.getDayOfMonth()))
                .contains(String.valueOf(opensOn.getYear()));
    }

    // --- fixtures -----------------------------------------------------------------------------

    /** A live class with one scheduled session and seats to spare, open for the dates given. */
    private UUID classOpenBetween(LocalDate opensOn, LocalDate closesOn) {
        ClassDefinition definition = new ClassDefinition();
        definition.setTitle("Weekend Data Analysis Bootcamp");
        definition.setDefaultInstructorUuid(instructorUuid);
        definition.setLocationType(LocationType.ONLINE);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setSessionFormat(SessionFormat.GROUP);
        definition.setMaxParticipants(20);
        definition.setAllowWaitlist(true);
        definition.setIsActive(true);
        definition.setRateBasis(RateBasis.PER_HOUR);
        definition.setSalePrice(new BigDecimal("1000.00"));
        definition.setDefaultStartTime(LocalDateTime.now(ZoneOffset.UTC).plusDays(45).withNano(0));
        definition.setDefaultEndTime(LocalDateTime.now(ZoneOffset.UTC).plusDays(45).plusHours(2).withNano(0));
        definition.setRegistrationPeriodStartDate(opensOn);
        definition.setRegistrationPeriodEndDate(closesOn);
        ClassDefinition saved = classDefinitionRepository.saveAndFlush(definition);

        ScheduledInstance instance = new ScheduledInstance();
        instance.setClassDefinitionUuid(saved.getUuid());
        instance.setInstructorUuid(instructorUuid);
        instance.setStartTime(LocalDateTime.now(ZoneOffset.UTC).plusDays(45).withNano(0));
        instance.setEndTime(LocalDateTime.now(ZoneOffset.UTC).plusDays(45).plusHours(2).withNano(0));
        instance.setTimezone("UTC");
        instance.setTitle(saved.getTitle());
        instance.setLocationType(LocationType.ONLINE.name());
        instance.setMaxParticipants(20);
        instance.setStatus(SchedulingStatus.SCHEDULED);
        scheduledInstanceRepository.saveAndFlush(instance);

        return saved.getUuid();
    }

    /** The seat reservation checkout takes, as it exists between paying and being enrolled. */
    private void holdASeatFor(UUID classDefinitionUuid, LocalDateTime heldUntil) {
        ScheduledInstance instance = scheduledInstanceRepository.findByClassDefinitionUuid(classDefinitionUuid).get(0);
        Enrollment hold = new Enrollment();
        hold.setScheduledInstanceUuid(instance.getUuid());
        hold.setStudentUuid(studentUuid);
        hold.setStatus(EnrollmentStatus.RESERVED);
        hold.setReservedUntil(heldUntil);
        enrollmentRepository.saveAndFlush(hold);
    }

    /** An open cart holding one seat on the class, owned by the buyer, ready to be completed. */
    private void stageCheckoutFor(UUID classDefinitionUuid) {
        CommerceProduct product = new CommerceProduct();
        product.setClassDefinitionUuid(classDefinitionUuid);
        product.setTitle("Weekend Data Analysis Bootcamp");

        CommerceProductVariant variant = new CommerceProductVariant();
        variant.setProduct(product);
        variant.setCode(classDefinitionUuid.toString());
        variant.setTitle("Standard seat");
        variant.setCurrencyCode("KES");

        CommerceCart cart = new CommerceCart();
        cart.setUuid(UUID.fromString(CART_ID));
        cart.setUserUuid(buyerUserUuid);
        cart.setCustomerEmail("buyer@example.com");
        cart.setCurrencyCode("KES");
        cart.setStatus(CartStatus.OPEN);

        CommerceCartItem item = new CommerceCartItem();
        item.setCart(cart);
        item.setVariant(variant);
        item.setQuantity(1);
        item.setUnitAmount(new BigDecimal("1000.00"));
        item.setSubtotalAmount(new BigDecimal("1000.00"));
        item.setTotalAmount(new BigDecimal("1000.00"));
        cart.setItems(new ArrayList<>(List.of(item)));

        when(cartRepository.findByUuid(UUID.fromString(CART_ID))).thenReturn(Optional.of(cart));
        when(classCapacityService.hasCapacity(classDefinitionUuid)).thenReturn(true);
        when(orderRepository.save(any(CommerceOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(commerceMapper.toOrderResponse(any(CommerceOrder.class))).thenReturn(null);
    }
}
