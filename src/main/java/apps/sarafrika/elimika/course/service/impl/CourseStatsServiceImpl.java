package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseReviewDTO;
import apps.sarafrika.elimika.course.dto.CourseStatsDTO;
import apps.sarafrika.elimika.course.dto.CourseStatsOwnerDTO;
import apps.sarafrika.elimika.course.dto.CourseStatsPublicDTO;
import apps.sarafrika.elimika.course.dto.CourseStatsScopedDTO;
import apps.sarafrika.elimika.course.internal.security.CourseFootingCap;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.service.CourseReviewService;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.CourseStatsService;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseContentAccess;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.CourseClassScope;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import apps.sarafrika.elimika.shared.spi.revenue.CommerceRevenueQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.CourseSalesSummary;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Builds the three statistics blocks, deciding entitlement block by block.
 * <p>
 * The decisions are made here rather than in a {@code @PreAuthorize} expression because they are not
 * one yes-or-no: the same request yields a public block for everyone, a scoped block for some, and
 * an owner block for fewer still. What an unentitled caller gets is not a filtered block but no
 * block, so nothing they may not see is ever serialised.
 * <p>
 * Each of those decisions is additionally capped by the dashboard the request came from, through
 * {@link CourseFootingCap}. This endpoint reads no {@code CourseContentAccess} and never consulted
 * the content resolver, so it carried its own separate instance of the same defect: a platform
 * administrator browsing their own learner dashboard was handed the owner block — gross sales, the
 * platform fee, the paid and refunded order counts — on a course they had no relationship with.
 * Capping the content resolver alone would have closed the syllabus and left the money open.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseStatsServiceImpl implements CourseStatsService {

    private static final String COURSE_NOT_FOUND_TEMPLATE = "Course with UUID %s not found";

    /**
     * Percentage steps the seat-fill figure is rounded to.
     * <p>
     * Coarsening is the point. A course's price is public, so an exact fill percentage against a
     * known class size lets anybody reconstruct the seats sold and multiply. Five-point buckets keep
     * the figure useful to a school sizing up demand and useless as an arithmetic input.
     */
    private static final int FILL_ROUNDING_STEP = 5;

    /**
     * Organisation-scoped roles that make somebody part of an organisation's teaching side.
     * <p>
     * Mirrors the set {@code CourseSecurityServiceImpl} uses. Plain membership is not enough: an
     * organisation's roster mixes staff with the learners it enrolled, and a training approval
     * granted to the organisation must reach its staff, never everybody it ever invited.
     */
    private static final List<UserDomain> ORGANISATION_TEACHING_DOMAINS = List.of(
            UserDomain.organisation_user, UserDomain.admin,
            UserDomain.instructor, UserDomain.course_creator);

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final CourseService courseService;
    private final CourseReviewService courseReviewService;
    private final CourseSecuritySpi courseSecurityService;
    private final DomainSecurityService domainSecurityService;
    private final CourseFootingCap courseFootingCap;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final EnrollmentLookupService enrollmentLookupService;
    private final CommerceRevenueQueryService commerceRevenueQueryService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;

    @Override
    @Transactional(readOnly = true)
    public CourseStatsDTO getCourseStats(UUID courseUuid) {
        if (courseUuid == null || !courseRepository.existsByUuid(courseUuid)) {
            throw new ResourceNotFoundException(String.format(COURSE_NOT_FOUND_TEMPLATE, courseUuid));
        }

        CourseStatsPublicDTO publicStats = publicStats(courseUuid);

        TrainerScope trainer = approvedTrainerScope(courseUuid);
        CourseStatsScopedDTO scoped = trainer.approved() ? scopedStats(courseUuid, trainer) : null;

        // Capped rather than merely tested: each clause keeps its own real check, and the dashboard
        // only decides which of them may still speak for this request.
        boolean readsCommerce = courseFootingCap.permitsCommercials(
                courseSecurityService.isCourseOwner(courseUuid),
                domainSecurityService.isPlatformAdmin());
        CourseStatsOwnerDTO owner = readsCommerce ? ownerStats(courseUuid) : null;

        return new CourseStatsDTO(publicStats, scoped, owner);
    }

    // ===== PUBLIC BLOCK =====

    private CourseStatsPublicDTO publicStats(UUID courseUuid) {
        CourseClassScope classes = classDefinitionLookupService.findClassScopeForCourse(courseUuid);

        // Learners trained spans every class the course has ever run; classes running and seat fill
        // describe only what is open now, which is why the scope carries both lists.
        long learnersTrained = enrollmentLookupService
                .countDistinctLearnersForClassDefinitions(classes.allClassUuids());
        long filledSeats = enrollmentLookupService
                .countFilledSeatsForClassDefinitions(classes.activeClassUuids());

        List<CourseReviewDTO> reviews = courseReviewService.getReviewsForCourse(courseUuid);

        return new CourseStatsPublicDTO(
                learnersTrained,
                classes.activeClassUuids().size(),
                averageClassFill(filledSeats, classes.activeSeatCapacity()),
                courseService.getCourseCompletionRate(courseUuid),
                averageRating(reviews),
                reviews.size(),
                courseTrainingApplicationRepository.countByCourseUuidAndStatus(
                        courseUuid, CourseTrainingApplicationStatus.APPROVED));
    }

    /**
     * Seat fill as a percentage rounded to the nearest {@value #FILL_ROUNDING_STEP}, clamped to
     * 0-100 so that classes over-filled from a waitlist do not report more than a full house.
     */
    private static int averageClassFill(long filledSeats, long seatCapacity) {
        if (seatCapacity <= 0L) {
            return 0;
        }
        double percentage = (filledSeats * 100.0d) / seatCapacity;
        int rounded = (int) (Math.round(percentage / FILL_ROUNDING_STEP) * FILL_ROUNDING_STEP);
        return Math.max(0, Math.min(100, rounded));
    }

    private static double averageRating(List<CourseReviewDTO> reviews) {
        OptionalDouble average = reviews.stream()
                .map(CourseReviewDTO::rating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        return average.orElse(0.0d);
    }

    // ===== SCOPED BLOCK =====

    private CourseStatsScopedDTO scopedStats(UUID courseUuid, TrainerScope trainer) {
        CourseClassScope classes = classDefinitionLookupService.findClassScopeForCourseAndTrainer(
                courseUuid, trainer.instructorUuid(), trainer.organisationUuids());

        return new CourseStatsScopedDTO(
                enrollmentLookupService.countDistinctLearnersForClassDefinitions(classes.allClassUuids()),
                classes.activeClassUuids().size(),
                commerceRevenueQueryService.sumCapturedCreditsForClassDefinitions(classes.allClassUuids()));
    }

    /**
     * The footings on which the caller is approved to deliver this course: their own instructor
     * approval, and approvals held by organisations whose teaching side they are on.
     * <p>
     * Only {@code APPROVED} counts. Anybody may submit an application and thereby put themselves in
     * {@code PENDING}, so treating pending as a footing would let a caller unlock the scoped block
     * on any course on the platform by asking for it.
     * <p>
     * Both sides are capped by the acting dashboard, and separately, because they answer different
     * questions: an instructor's page reports what that instructor teaches, a school's page what the
     * school teaches, and neither is a fact a learner's or a guardian's page has any business
     * carrying. A dashboard that cannot reach the footing never runs the lookup behind it.
     */
    private TrainerScope approvedTrainerScope(UUID courseUuid) {
        try {
            UUID userUuid = domainSecurityService.getCurrentUserUuid();
            if (userUuid == null) {
                return TrainerScope.none();
            }

            UUID instructorUuid = courseFootingCap.permits(CourseContentAccess.INSTRUCTOR)
                    ? instructorLookupService.findInstructorUuidByUserUuid(userUuid)
                            .filter(candidate -> isApprovedApplicant(
                                    courseUuid, CourseTrainingApplicantType.INSTRUCTOR, candidate))
                            .orElse(null)
                    : null;

            List<UUID> organisationUuids = courseFootingCap.permits(CourseContentAccess.ORGANISATION)
                    ? userLookupService.getActiveUserOrganizations(userUuid).stream()
                            .filter(this::teachesFor)
                            .filter(organisationUuid -> isApprovedApplicant(
                                    courseUuid, CourseTrainingApplicantType.ORGANISATION, organisationUuid))
                            .toList()
                    : List.<UUID>of();

            return new TrainerScope(instructorUuid, organisationUuids);
        } catch (Exception e) {
            log.error("Error resolving the training approvals held on course {}", courseUuid, e);
            return TrainerScope.none();
        }
    }

    private boolean isApprovedApplicant(UUID courseUuid, CourseTrainingApplicantType type, UUID applicantUuid) {
        return applicantUuid != null
                && courseTrainingApplicationRepository.existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                        courseUuid, type, applicantUuid, CourseTrainingApplicationStatus.APPROVED);
    }

    private boolean teachesFor(UUID organisationUuid) {
        return ORGANISATION_TEACHING_DOMAINS.stream()
                .anyMatch(domain -> domainSecurityService.belongsToOrganisationWithDomain(organisationUuid, domain));
    }

    // ===== OWNER BLOCK =====

    private CourseStatsOwnerDTO ownerStats(UUID courseUuid) {
        CourseSalesSummary sales = commerceRevenueQueryService.summariseSalesForCourse(courseUuid);
        return new CourseStatsOwnerDTO(
                courseEnrollmentRepository.countByCourseUuid(courseUuid),
                sales.grossSales(),
                sales.platformFee(),
                sales.paidOrders(),
                sales.refundedOrders());
    }

    /**
     * Which classes on this course belong to the caller, and whether they may see any at all.
     *
     * @param instructorUuid    the caller's instructor identity when it holds an approval, else null
     * @param organisationUuids organisations the caller teaches for that hold an approval
     */
    private record TrainerScope(UUID instructorUuid, List<UUID> organisationUuids) {

        static TrainerScope none() {
            return new TrainerScope(null, List.of());
        }

        boolean approved() {
            return instructorUuid != null || !organisationUuids.isEmpty();
        }
    }
}
