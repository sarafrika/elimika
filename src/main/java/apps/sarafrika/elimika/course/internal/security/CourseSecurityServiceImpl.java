package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.model.Certificate;
import apps.sarafrika.elimika.course.model.AssignmentSubmissionAttachment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.CourseTrainingApplication;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramRequirement;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionAttachmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.CertificateRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.CourseTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramRequirementRepository;
import apps.sarafrika.elimika.course.repository.ProgramTrainingApplicationRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.repository.projection.MaterialCourseView;
import apps.sarafrika.elimika.course.spi.CourseSecuritySpi;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicationStatus;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Internal implementation of course security operations.
 * Provides authorization checks for course ownership.
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
@Service("courseSecurityService")
@RequiredArgsConstructor
@Slf4j
public class CourseSecurityServiceImpl implements CourseSecuritySpi {

    private static final String CACHE_ENROLLED_COURSES = "courseSecurity.enrolledCourses";
    private static final String CACHE_QUIZ_COURSE = "courseSecurity.quizCourse.";
    private static final String CACHE_ASSIGNMENT_COURSE = "courseSecurity.assignmentCourse.";
    private static final String CACHE_MANAGEABLE_COURSES = "courseSecurity.manageableCourses";
    private static final String CACHE_INSTRUCTOR_COURSES = "courseSecurity.instructorCourses.";
    private static final String CACHE_OWNED_PROGRAM_PREFIX = "courseSecurity.ownsProgram.";
    private static final String CACHE_TEACHABLE_COURSES = "courseSecurity.teachableCourses";
    private static final String CACHE_TEACHES_STUDENT_PREFIX = "courseSecurity.teachesStudent.";

    /**
     * Organisation-scoped roles that make a member part of an organisation's <em>teaching</em> side.
     * <p>
     * Membership alone is not one of them. An organisation's roster mixes its staff with the
     * learners it enrolled, both carried by rows in the same table, and only the org-scoped domain
     * tells them apart — so a training approval granted to an organisation must be read as granting
     * its staff, never everyone it has ever invited.
     */
    private static final List<UserDomain> ORGANISATION_TEACHING_DOMAINS = List.of(
            UserDomain.organisation_user, UserDomain.admin,
            UserDomain.instructor, UserDomain.course_creator);

    /**
     * The org-scoped roles that make somebody staff of an organisation rather than one of its
     * learners. Mirrors the set the organisation invitation flow issues, minus {@code student}.
     */
    private static final List<UserDomain> ORGANISATION_STAFF_DOMAINS =
            List.of(UserDomain.instructor, UserDomain.organisation_user, UserDomain.admin);

    private final CourseRepository courseRepository;
    private final CourseTrainingApplicationRepository courseTrainingApplicationRepository;
    private final ProgramTrainingApplicationRepository programTrainingApplicationRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final QuizRepository quizRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentSubmissionAttachmentRepository assignmentSubmissionAttachmentRepository;
    private final CertificateRepository certificateRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ProgramRequirementRepository programRequirementRepository;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final InstructorLookupService instructorLookupService;
    private final UserLookupService userLookupService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * Checks if the currently authenticated user is the owner of the specified course.
     *
     * Flow: JWT (keycloakId) → User → CourseCreator → Course
     *
     * @param courseUuid UUID of the course to check
     * @return true if the current user owns the course, false otherwise
     */
    @Override
    public boolean isCourseOwner(UUID courseUuid) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authenticated user found");
                return false;
            }

            // Get Keycloak ID from JWT
            String keycloakId = getKeycloakId(authentication);
            if (keycloakId == null) {
                log.debug("Could not extract Keycloak ID from authentication");
                return false;
            }

            // Find User by Keycloak ID via SPI
            UUID userUuid = userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            if (userUuid == null) {
                log.debug("User not found for Keycloak ID: {}", keycloakId);
                return false;
            }

            // Find CourseCreator by User UUID via SPI
            UUID courseCreatorUuid = courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).orElse(null);
            if (courseCreatorUuid == null) {
                log.debug("CourseCreator not found for user UUID: {}", userUuid);
                return false;
            }

            // Get course and check ownership
            Course course = courseRepository.findByUuid(courseUuid).orElse(null);
            if (course == null) {
                log.debug("Course not found: {}", courseUuid);
                return false;
            }

            boolean isOwner = course.getCourseCreatorUuid() != null &&
                             course.getCourseCreatorUuid().equals(courseCreatorUuid);

            log.debug("Course ownership check for user {} (courseCreator {}) on course {}: {}",
                     userUuid, courseCreatorUuid, courseUuid, isOwner);

            return isOwner;

        } catch (Exception e) {
            log.error("Error checking course ownership for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * Grants content read access to the course owner or a member of an organisation
     * approved to train the course. Admins are handled at the endpoint; enrolled
     * learners use their own class flow, not this path.
     */
    @Override
    public boolean canReadCourseContent(UUID courseUuid) {
        if (isCourseOwner(courseUuid)) {
            return true;
        }
        try {
            UUID userUuid = currentUserUuid();
            if (userUuid == null) {
                return false;
            }
            return belongsToApprovedTrainingOrganisation(courseUuid, userUuid);
        } catch (Exception e) {
            log.error("Error checking content read access for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * True when the caller holds a course enrolment for this course that still allows access.
     * <p>
     * Resolves the student from the authenticated principal, never from a request parameter, so
     * a learner cannot claim somebody else's enrolment. {@code course_enrollments} is unique on
     * (student, course), so the lookup is unambiguous.
     */
    @Override
    public boolean isEnrolledLearner(UUID courseUuid) {
        return courseUuid != null && enrolledCourseUuids().contains(courseUuid);
    }

    /**
     * The caller's enrolable courses, loaded once per request.
     * <p>
     * A learner holds a handful of enrolments, so fetching the whole set costs one query and then
     * answers every course check by set membership. Checking course-by-course instead would cost a
     * query per course, which a page listing material from several courses pays repeatedly.
     */
    @Override
    public Set<UUID> enrolledCourseUuids() {
        return requestScopedCache.get(CACHE_ENROLLED_COURSES, () -> {
            try {
                UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
                if (studentUuid == null) {
                    return Set.<UUID>of();
                }
                return Set.copyOf(courseEnrollmentRepository.findCourseUuidsByStudentUuidAndStatusIn(
                        studentUuid, EnrollmentStatus.ACCESS_ALLOWING));
            } catch (Exception e) {
                log.error("Error loading enrolled courses for the current caller", e);
                return Set.<UUID>of();
            }
        });
    }

    /**
     * Staff reading rights plus enrolled learners. See the SPI javadoc for why this is a sibling
     * of {@link #canReadCourseContent(UUID)} rather than a widening of it.
     */
    @Override
    public boolean canReadCourseAsLearner(UUID courseUuid) {
        return canReadCourseContent(courseUuid) || isEnrolledLearner(courseUuid);
    }

    /**
     * Grants gradebook access only where there is a real relationship to the course.
     * <p>
     * Deliberately stricter than a role check: holding the instructor or course_creator
     * domain says nothing about whether this particular course is yours to mark. Answered from
     * {@link #manageableCourseUuids()} so that the single-course question and the narrowing of a
     * listing can never drift apart.
     */
    @Override
    public boolean canManageCourseGradebook(UUID courseUuid) {
        return courseUuid != null && manageableCourseUuids().contains(courseUuid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Loaded once per request: a page that checks a dozen quizzes and assignments asks this
     * question a dozen times, and the underlying joins do not change inside one request.
     */
    @Override
    public Set<UUID> manageableCourseUuids() {
        return requestScopedCache.get(CACHE_MANAGEABLE_COURSES, () -> manageableCoursesForUser(currentUserUuid()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<UUID> manageableCourseUuidsForInstructor(UUID instructorUuid) {
        if (instructorUuid == null) {
            return Set.of();
        }
        return requestScopedCache.get(CACHE_INSTRUCTOR_COURSES + instructorUuid, () -> {
            try {
                return manageableCoursesForUser(
                        instructorLookupService.getInstructorUserUuid(instructorUuid).orElse(null));
            } catch (Exception e) {
                log.error("Error resolving the courses instructor {} may mark", instructorUuid, e);
                return Set.<UUID>of();
            }
        });
    }

    /**
     * The four footings on which a user may mark a course, unioned: authorship, a personal training
     * approval, an approval held by an organisation they teach for, and a programme-level approval
     * on either footing, which covers every course inside that programme.
     * <p>
     * Programmes matter because a class may be scheduled against one instead of a course, and the
     * instructor approved to deliver it holds no course-level application for its courses at all.
     * Anything that throws resolves to "nothing", so a lookup failure denies rather than grants.
     */
    private Set<UUID> manageableCoursesForUser(UUID userUuid) {
        if (userUuid == null) {
            return Set.of();
        }
        try {
            Set<UUID> courses = new HashSet<>();

            courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid)
                    .map(courseRepository::findUuidsByCourseCreatorUuid)
                    .ifPresent(courses::addAll);

            List<UUID> asInstructor = instructorLookupService.findInstructorUuidByUserUuid(userUuid)
                    .map(List::of)
                    .orElseGet(List::of);
            List<UUID> asOrganisation = teachingOrganisationsOf(userUuid);

            courses.addAll(approvedCourses(CourseTrainingApplicantType.INSTRUCTOR, asInstructor));
            courses.addAll(approvedCourses(CourseTrainingApplicantType.ORGANISATION, asOrganisation));

            Set<UUID> programmes = new HashSet<>();
            programmes.addAll(approvedProgrammes(CourseTrainingApplicantType.INSTRUCTOR, asInstructor));
            programmes.addAll(approvedProgrammes(CourseTrainingApplicantType.ORGANISATION, asOrganisation));
            if (!programmes.isEmpty()) {
                courses.addAll(programCourseRepository.findCourseUuidsByProgramUuidIn(programmes));
            }

            return Set.copyOf(courses);
        } catch (Exception e) {
            log.error("Error resolving the courses user {} may mark", userUuid, e);
            return Set.of();
        }
    }

    /**
     * Organisations this user belongs to <em>as staff</em>. See
     * {@link #ORGANISATION_TEACHING_DOMAINS} for why plain membership is not enough.
     */
    private List<UUID> teachingOrganisationsOf(UUID userUuid) {
        return userLookupService.getUserOrganizations(userUuid).stream()
                .filter(organisationUuid -> ORGANISATION_TEACHING_DOMAINS.stream()
                        .anyMatch(domain -> userLookupService.userBelongsToOrganizationWithDomain(
                                userUuid, organisationUuid, domain)))
                .toList();
    }

    private List<UUID> approvedCourses(CourseTrainingApplicantType applicantType, Collection<UUID> applicants) {
        return applicants.isEmpty()
                ? List.of()
                : courseTrainingApplicationRepository.findApprovedCourseUuids(
                        applicantType, applicants, CourseTrainingApplicationStatus.APPROVED);
    }

    private List<UUID> approvedProgrammes(CourseTrainingApplicantType applicantType, Collection<UUID> applicants) {
        return applicants.isEmpty()
                ? List.of()
                : programTrainingApplicationRepository.findApprovedProgramUuids(
                        applicantType, applicants, CourseTrainingApplicationStatus.APPROVED);
    }

    /**
     * Whether the caller may see and change the inside of a quiz: its questions, the {@code
     * is_correct} flags on their options, and every learner's attempts at it.
     * <p>
     * A quiz hangs off a lesson rather than a course, so the decision is one join away — resolve the
     * owning course, then defer to {@link #canManageCourseGradebook(UUID)}. The question an answer
     * key poses is not "does this caller teach somewhere", which every instructor on the platform
     * satisfies, but "is this quiz theirs to mark".
     * <p>
     * Not on {@link CourseSecuritySpi}: these are course-module-internal predicates reached from
     * {@code @PreAuthorize} by bean name, and the SPI stays the cross-module contract.
     *
     * @param quizUuid UUID of the quiz to check
     * @return true when the caller owns or is approved to train the quiz's course
     */
    public boolean canManageQuiz(UUID quizUuid) {
        return materialCourse(CACHE_QUIZ_COURSE, quizUuid, quizRepository::findCourseViewByUuid)
                .map(material -> canManageCourseGradebook(material.courseUuid()))
                .orElse(false);
    }

    /**
     * The assignment counterpart of {@link #canManageQuiz(UUID)}: whether this assignment's
     * submissions, scores and per-assignment analytics are the caller's to read and to write.
     *
     * @param assignmentUuid UUID of the assignment to check
     * @return true when the caller owns or is approved to train the assignment's course
     */
    public boolean canManageAssignment(UUID assignmentUuid) {
        return materialCourse(CACHE_ASSIGNMENT_COURSE, assignmentUuid, assignmentRepository::findCourseViewByUuid)
                .map(material -> canManageCourseGradebook(material.courseUuid()))
                .orElse(false);
    }

    /**
     * Whether the caller may act on, or ask about, one assignment submission as its marker: the
     * assignment's course is theirs, <em>and</em> the submission is on that assignment.
     * <p>
     * Without the second half the assignment in the path is decoration. The guard would read
     * {@code #assignmentUuid} while the handler wrote to {@code #submissionUuid}, so naming any
     * assignment you legitimately mark would let you put a score and a comment on any submission on
     * the platform.
     *
     * @param assignmentUuid UUID of the assignment named in the request path
     * @param submissionUuid UUID of the submission being acted on
     * @return true when the caller marks this assignment and the submission belongs to it
     */
    public boolean canManageAssignmentSubmission(UUID assignmentUuid, UUID submissionUuid) {
        if (assignmentUuid == null || submissionUuid == null || !canManageAssignment(assignmentUuid)) {
            return false;
        }
        try {
            return assignmentSubmissionRepository.findByUuid(submissionUuid)
                    .map(submission -> assignmentUuid.equals(submission.getAssignmentUuid()))
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error binding submission {} to assignment {}", submissionUuid, assignmentUuid, e);
            return false;
        }
    }

    /**
     * Whether the caller holds a course enrolment for this quiz's course, whatever state it is in.
     * <p>
     * This is the learner half of the results endpoints, and it is deliberately weaker than
     * {@link #isEnrolledLearner(UUID)}: that answers "may I still enter this course", which
     * a dropped or suspended learner may not, while this answers "is this course part of my
     * record", which stays true for as long as the enrolment exists. Losing access to a course
     * must not delete the learner's own marks from their view, and the same reasoning covers
     * unpublishing — a quiz withdrawn after the fact does not un-sit the attempt.
     * <p>
     * It grants the question, not the answer: which rows come back is separately narrowed to the
     * caller's own enrolments by {@code LearnerAssessmentScope}. Both halves are needed.
     *
     * @param quizUuid UUID of the quiz to check
     * @return true when the caller has a course enrolment on the quiz's course
     */
    public boolean hasCourseEnrollmentForQuiz(UUID quizUuid) {
        return materialCourse(CACHE_QUIZ_COURSE, quizUuid, quizRepository::findCourseViewByUuid)
                .map(material -> holdsCourseEnrollment(material.courseUuid()))
                .orElse(false);
    }

    /**
     * The assignment counterpart of {@link #hasCourseEnrollmentForQuiz(UUID)}.
     *
     * @param assignmentUuid UUID of the assignment to check
     * @return true when the caller has a course enrolment on the assignment's course
     */
    public boolean hasCourseEnrollmentForAssignment(UUID assignmentUuid) {
        return materialCourse(CACHE_ASSIGNMENT_COURSE, assignmentUuid, assignmentRepository::findCourseViewByUuid)
                .map(material -> holdsCourseEnrollment(material.courseUuid()))
                .orElse(false);
    }

    /**
     * Whether a submitted file may be served to the caller, given only its storage path.
     * <p>
     * A path is not an authorization subject, so the decision starts by resolving it back to the
     * attachment row and thence to the submission that owns it — the same question the attachment
     * listing answers, asked one indirection later. An unknown path is refused: nothing that is not
     * a recorded submission attachment is served through this route.
     *
     * @param filePath stored relative path of the submitted file
     * @return true when the caller owns the submission or marks its assignment
     */
    public boolean canReadSubmissionMedia(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        try {
            String storagePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            return assignmentSubmissionAttachmentRepository.findByStoragePath(storagePath).stream()
                    .map(AssignmentSubmissionAttachment::getSubmissionUuid)
                    .anyMatch(this::canReachSubmission);
        } catch (Exception e) {
            log.error("Error checking submission media access for path: {}", filePath, e);
            return false;
        }
    }

    /**
     * True when the caller either handed this submission in or marks the assignment it answers.
     */
    private boolean canReachSubmission(UUID submissionUuid) {
        if (ownsAssignmentSubmission(submissionUuid)) {
            return true;
        }
        return assignmentSubmissionRepository.findByUuid(submissionUuid)
                .map(AssignmentSubmission::getAssignmentUuid)
                .map(this::canManageAssignment)
                .orElse(false);
    }

    /**
     * True when the caller has any course enrolment on this course, in any state.
     */
    private boolean holdsCourseEnrollment(UUID courseUuid) {
        if (courseUuid == null) {
            return false;
        }
        try {
            UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
            return studentUuid != null
                    && courseEnrollmentRepository.existsByStudentUuidAndCourseUuid(studentUuid, courseUuid);
        } catch (Exception e) {
            log.error("Error checking course enrolment for course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * Whether this assignment submission is the calling learner's own work.
     * <p>
     * Submissions are keyed by course enrolment rather than by student, so proving ownership means
     * walking submission → enrolment → student and comparing against the authenticated principal,
     * never against a request parameter. Enrolment status is deliberately not consulted: a learner
     * who has since dropped the course still owns the files they handed in.
     *
     * @param submissionUuid UUID of the assignment submission
     * @return true when the submission belongs to the caller's own course enrolment
     */
    public boolean ownsAssignmentSubmission(UUID submissionUuid) {
        if (submissionUuid == null) {
            return false;
        }
        try {
            UUID studentUuid = domainSecurityService.getCurrentStudentUuid();
            if (studentUuid == null) {
                return false;
            }
            return assignmentSubmissionRepository.findByUuid(submissionUuid)
                    .map(AssignmentSubmission::getEnrollmentUuid)
                    .flatMap(courseEnrollmentRepository::findByUuid)
                    .map(CourseEnrollment::getStudentUuid)
                    .map(studentUuid::equals)
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error checking submission ownership for submission: {}", submissionUuid, e);
            return false;
        }
    }

    /**
     * Resolves the course that owns a piece of assessment material, memoised for the request.
     * <p>
     * Every guarded assessment route pays this walk before its handler runs, and a page that lists
     * a quiz's questions then fetches each question's options asks about the same quiz repeatedly.
     * Caching the projection keeps that at one query. Anything that fails to resolve stays empty,
     * so the callers above deny rather than guess.
     */
    private Optional<MaterialCourseView> materialCourse(String cachePrefix, UUID uuid,
                                                        Function<UUID, Optional<MaterialCourseView>> resolver) {
        if (uuid == null) {
            return Optional.empty();
        }
        return requestScopedCache.get(cachePrefix + uuid, () -> {
            try {
                return resolver.apply(uuid);
            } catch (Exception e) {
                log.error("Error resolving the course owning assessment material: {}", uuid, e);
                return Optional.<MaterialCourseView>empty();
            }
        });
    }

    // ===== training-program ownership and program-scoped writes (unit u08) =====

    /**
     * Checks if the currently authenticated user created the specified training program.
     * <p>
     * A program is owned the way a course is, through its creator's profile, so this resolves
     * JWT → User → profile → TrainingProgram exactly as {@link #isCourseOwner(UUID)} does.
     * <p>
     * Two profiles have to be tried, not one. The course-creator dashboard stamps
     * {@code course_creator_uuid} with the caller's course-creator profile, but the instructor
     * dashboard's program builder stamps it with the caller's <em>instructor</em> profile, so
     * programs authored there are on record under an instructor UUID. Accepting either is what
     * keeps an instructor able to edit, publish and delete the programs they built; narrowing to
     * the course-creator profile alone would lock them out of their own work.
     * <p>
     * Memoised per request and per program because the route guard and the service behind it both
     * ask, and each answer costs a program load.
     *
     * @param programUuid UUID of the training program to check
     * @return true if the current user created the program, false otherwise
     */
    @Override
    public boolean isProgramOwner(UUID programUuid) {
        if (programUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_OWNED_PROGRAM_PREFIX + programUuid, () -> {
            try {
                UUID userUuid = currentUserUuid();
                if (userUuid == null) {
                    return false;
                }
                Set<UUID> creatorIdentities = programCreatorIdentities(userUuid);
                if (creatorIdentities.isEmpty()) {
                    return false;
                }
                return trainingProgramRepository.findByUuid(programUuid)
                        .map(program -> program.getCourseCreatorUuid() != null
                                && creatorIdentities.contains(program.getCourseCreatorUuid()))
                        .orElse(false);
            } catch (Exception e) {
                log.error("Error checking program ownership for program: {}", programUuid, e);
                return false;
            }
        });
    }

    /**
     * The profile UUIDs a training program's {@code course_creator_uuid} may legitimately hold for
     * this user - their course-creator profile and their instructor profile. See
     * {@link #isProgramOwner(UUID)} for why both count.
     * <p>
     * Only ever asked of the caller, so the instructor half comes from the caller's memoised
     * instructor identity rather than a fresh lookup per program.
     */
    private Set<UUID> programCreatorIdentities(UUID userUuid) {
        Set<UUID> identities = new HashSet<>();
        courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).ifPresent(identities::add);
        UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
        if (instructorUuid != null) {
            identities.add(instructorUuid);
        }
        return identities;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The association is resolved first and its own program decides, because
     * {@code ProgramCourseServiceImpl} writes the row named by {@code programCourseUuid} and never
     * looks at the path - so trusting the path would let a caller quote a program they own alongside
     * a stranger's row.
     */
    @Override
    public boolean canWriteProgramCourse(UUID programUuid, UUID programCourseUuid, UUID payloadProgramUuid) {
        UUID owningProgramUuid = programCourseUuid == null
                ? programUuid
                : programCourseRepository.findByUuid(programCourseUuid)
                        .map(ProgramCourse::getProgramUuid)
                        .orElse(programUuid);
        return ownsBothPrograms(owningProgramUuid, payloadProgramUuid);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resolved exactly as {@link #canWriteProgramCourse(UUID, UUID, UUID)} is, and for the same
     * reason: {@code ProgramRequirementServiceImpl} writes the row named by {@code requirementUuid}
     * and ignores the path.
     */
    @Override
    public boolean canWriteProgramRequirement(UUID programUuid, UUID requirementUuid, UUID payloadProgramUuid) {
        UUID owningProgramUuid = requirementUuid == null
                ? programUuid
                : programRequirementRepository.findByUuid(requirementUuid)
                        .map(ProgramRequirement::getProgramUuid)
                        .orElse(programUuid);
        return ownsBothPrograms(owningProgramUuid, payloadProgramUuid);
    }

    /**
     * The caller must own the program the row lives in and, when the body names a program to move it
     * to, own that one too - otherwise a program's courses or requirements could be pushed into a
     * stranger's program.
     */
    private boolean ownsBothPrograms(UUID owningProgramUuid, UUID payloadProgramUuid) {
        if (!isProgramOwner(owningProgramUuid)) {
            return false;
        }
        return payloadProgramUuid == null
                || payloadProgramUuid.equals(owningProgramUuid)
                || isProgramOwner(payloadProgramUuid);
    }

    // ===== certificate issuance, revocation and reads (unit u17) =====

    /**
     * True when the caller may mint, amend or revoke a certificate for this course or program.
     * <p>
     * A certificate is an assertion about somebody's achievement, so the right to make one is the
     * teaching right over the work it attests to: for a course, its author, an instructor approved
     * to train it, or the teaching staff of an organisation approved to train it; for a program,
     * its author. Platform admins are granted at the endpoint.
     * <p>
     * Exactly one of the two identifiers may be set. Neither means the record names nothing to be
     * authorised against; <em>both</em> means the payload names two subjects while the check could
     * only ever consider one, which is how a caller entitled to a course would otherwise mint an
     * assertion about somebody else's program. Both cases fail closed here, and the service rejects
     * them outright so the caller gets a 400 rather than a puzzling 403.
     *
     * @param courseUuid  the course the certificate attests to, or null for a program certificate
     * @param programUuid the program the certificate attests to, or null for a course certificate
     */
    public boolean canAwardCertificate(UUID courseUuid, UUID programUuid) {
        if (courseUuid != null && programUuid != null) {
            return false;
        }
        if (courseUuid != null) {
            return canTeachCourse(courseUuid);
        }
        if (programUuid != null) {
            return isProgramOwner(programUuid);
        }
        return false;
    }

    /**
     * True when the caller may read every certificate issued under a training program.
     * <p>
     * The listing is the program's whole cohort, grades included, so it belongs to whoever authors
     * the program. Platform admins are granted at the endpoint.
     *
     * @param programUuid the program whose certificates are being listed
     */
    public boolean canReadProgramCertificates(UUID programUuid) {
        return programUuid != null && isProgramOwner(programUuid);
    }

    /**
     * True when the caller actually teaches this course: its author, an instructor individually
     * approved to train it, or the teaching staff of an organisation approved to train it.
     * <p>
     * Deliberately narrower than {@link #canManageCourseGradebook(UUID)}, whose organisation branch
     * admits <em>any</em> member of an approved organisation. Organisations enrol their learners as
     * members too, so that branch would hand a student of an approved organisation the right to
     * issue certificates in its name. Here the organisation only carries the right through to
     * someone holding a teaching or management role in that same organisation.
     */
    private boolean canTeachCourse(UUID courseUuid) {
        if (isCourseOwner(courseUuid)) {
            return true;
        }
        try {
            UUID userUuid = currentUserUuid();
            if (userUuid == null) {
                return false;
            }

            // The caller's own instructor identity, resolved once per request.
            UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
            if (instructorUuid != null && courseTrainingApplicationRepository
                    .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                            courseUuid,
                            CourseTrainingApplicantType.INSTRUCTOR,
                            instructorUuid,
                            CourseTrainingApplicationStatus.APPROVED)) {
                return true;
            }

            for (UUID organisationUuid : staffedOrganisationUuids(userUuid)) {
                if (courseTrainingApplicationRepository
                        .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                                courseUuid,
                                CourseTrainingApplicantType.ORGANISATION,
                                organisationUuid,
                                CourseTrainingApplicationStatus.APPROVED)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking teaching rights over course: {}", courseUuid, e);
            return false;
        }
    }

    /**
     * The organisations the caller is <em>staff</em> of, as opposed to merely a member of.
     * <p>
     * Membership of an organisation covers its learners as much as its employees — the invitation
     * flow issues {@code student} alongside {@code instructor}, {@code organisation_user} and
     * {@code admin} — so anywhere an organisation's own rights are being passed through to a
     * person, the org-scoped role has to be read as well as the membership. Each domain is checked
     * against <em>that</em> organisation, never as a role held elsewhere.
     */
    private List<UUID> staffedOrganisationUuids(UUID userUuid) {
        return userLookupService.getUserOrganizations(userUuid).stream()
                .filter(organisationUuid -> isOrganisationStaff(userUuid, organisationUuid))
                .toList();
    }

    /**
     * True when the user holds a teaching or management role in this specific organisation.
     */
    private boolean isOrganisationStaff(UUID userUuid, UUID organisationUuid) {
        for (UserDomain domain : ORGANISATION_STAFF_DOMAINS) {
            if (userLookupService.userBelongsToOrganizationWithDomain(userUuid, organisationUuid, domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The same right as {@link #canAwardCertificate(UUID, UUID)}, resolved from an issued
     * certificate. An unknown certificate is not authorisable, so this is false.
     *
     * @param certificateUuid the certificate being amended, uploaded to or revoked
     */
    public boolean canManageCertificate(UUID certificateUuid) {
        Certificate certificate = findCertificate(certificateUuid);
        return certificate != null
                && canAwardCertificate(certificate.getCourseUuid(), certificate.getProgramUuid());
    }

    /**
     * True when the caller may read a single issued certificate — the learner it was awarded to,
     * the staff who could have awarded it, or a manager of an organisation the learner belongs to.
     * <p>
     * Reading matters as much as issuing here: the record carries the learner's final grade.
     * Platform admins are granted at the endpoint.
     *
     * @param certificateUuid the certificate being read
     */
    public boolean canReadCertificate(UUID certificateUuid) {
        return canRead(findCertificate(certificateUuid));
    }

    /**
     * {@link #canReadCertificate(UUID)} addressed by the printed certificate number.
     * <p>
     * The number is what a holder shows a third party, so the route that takes it is the one most
     * likely to be guessed at; it returns the whole record, grade included, which is why it is
     * guarded like any other read rather than left open. The boolean-only verification route is
     * the one meant for a stranger holding a certificate.
     *
     * @param certificateNumber the printed certificate number
     */
    public boolean canReadCertificateByNumber(String certificateNumber) {
        if (certificateNumber == null || certificateNumber.isBlank()) {
            return false;
        }
        try {
            return canRead(certificateRepository.findByCertificateNumber(certificateNumber).orElse(null));
        } catch (Exception e) {
            log.error("Error checking read access for a certificate number", e);
            return false;
        }
    }

    /**
     * True when the caller may read a learner's certificates as a list — the learner themselves, a
     * manager of an organisation they belong to, or staff who teach them.
     * <p>
     * "Teaches them" is deliberately a relationship and not a role: holding the instructor domain
     * says nothing about whether this particular learner's results are yours to see. The caller's
     * own teachable courses are loaded once and intersected with the learner's enrolments, so the
     * check costs a fixed handful of queries however many courses either side has. Platform admins
     * are granted at the endpoint.
     *
     * @param studentUuid the learner whose certificates are being listed
     */
    public boolean canReadStudentCertificates(UUID studentUuid) {
        if (studentUuid == null) {
            return false;
        }
        if (domainSecurityService.isStudentWithUuid(studentUuid)
                || domainSecurityService.managesOrganisationOfStudent(studentUuid)) {
            return true;
        }
        return teachesStudent(studentUuid);
    }

    /**
     * True when at least one course the learner is enrolled on is one the caller may teach.
     * <p>
     * Memoised per learner because an instructor's roster page asks the same question for every
     * learner it renders.
     */
    private boolean teachesStudent(UUID studentUuid) {
        return requestScopedCache.get(CACHE_TEACHES_STUDENT_PREFIX + studentUuid, () -> {
            try {
                Set<UUID> teachable = teachableCourseUuids();
                if (teachable.isEmpty()) {
                    return false;
                }
                return courseEnrollmentRepository
                        .findCourseUuidsByStudentUuidAndStatusIn(studentUuid, List.of(EnrollmentStatus.values()))
                        .stream()
                        .anyMatch(teachable::contains);
            } catch (Exception e) {
                log.error("Error checking teaching relationship with student {}", studentUuid, e);
                return false;
            }
        });
    }

    /**
     * Every course the caller may teach: the ones they authored, the ones they are individually
     * approved to train, and the ones an organisation they are <em>staff</em> of is approved to
     * train.
     * <p>
     * Loaded whole, once per request, so that questions of the form "does this caller teach any of
     * these courses" cost one load rather than a lookup per course. The organisation branch is
     * filtered by org-scoped role for the same reason issuance is: an organisation enrols its
     * learners as members, and a learner of an approved organisation is not thereby entitled to
     * every classmate's results.
     */
    private Set<UUID> teachableCourseUuids() {
        return requestScopedCache.get(CACHE_TEACHABLE_COURSES, () -> {
            try {
                UUID userUuid = currentUserUuid();
                if (userUuid == null) {
                    return Set.<UUID>of();
                }
                Set<UUID> courseUuids = new HashSet<>();
                courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid)
                        .ifPresent(courseCreatorUuid ->
                                courseUuids.addAll(courseRepository.findUuidsByCourseCreatorUuid(courseCreatorUuid)));
                UUID callerInstructorUuid = domainSecurityService.getCurrentInstructorUuid();
                if (callerInstructorUuid != null) {
                    addApprovedTrainingCourses(courseUuids, callerInstructorUuid);
                }
                for (UUID organisationUuid : staffedOrganisationUuids(userUuid)) {
                    addApprovedTrainingCourses(courseUuids, organisationUuid);
                }
                return Set.copyOf(courseUuids);
            } catch (Exception e) {
                log.error("Error loading teachable courses for the current caller", e);
                return Set.<UUID>of();
            }
        });
    }

    /**
     * Adds the courses an applicant — an instructor profile or an organisation — is approved to
     * train. Applicant identifiers are drawn from separate pools, so the type need not be repeated.
     */
    private void addApprovedTrainingCourses(Set<UUID> courseUuids, UUID applicantUuid) {
        for (CourseTrainingApplication application : courseTrainingApplicationRepository
                .findByApplicantUuidAndStatus(applicantUuid, CourseTrainingApplicationStatus.APPROVED)) {
            courseUuids.add(application.getCourseUuid());
        }
    }

    /**
     * Shared body of the certificate read checks, over an already-loaded record.
     */
    private boolean canRead(Certificate certificate) {
        if (certificate == null) {
            return false;
        }
        if (domainSecurityService.isStudentWithUuid(certificate.getStudentUuid())) {
            return true;
        }
        if (canAwardCertificate(certificate.getCourseUuid(), certificate.getProgramUuid())) {
            return true;
        }
        return domainSecurityService.managesOrganisationOfStudent(certificate.getStudentUuid());
    }

    /**
     * Loads a certificate for an authorization decision, treating any failure as "no such record".
     */
    private Certificate findCertificate(UUID certificateUuid) {
        if (certificateUuid == null) {
            return null;
        }
        try {
            return certificateRepository.findByUuid(certificateUuid).orElse(null);
        } catch (Exception e) {
            log.error("Error loading certificate {} for an authorization check", certificateUuid, e);
            return null;
        }
    }

    /**
     * True when the user belongs to an organisation approved to train this course.
     * <p>
     * Membership is enough here because this backs reading course <em>material</em>, which an
     * organisation's learners are meant to see. Marking is a different question and asks a stricter
     * one — see {@link #teachingOrganisationsOf(UUID)}.
     */
    private boolean belongsToApprovedTrainingOrganisation(UUID courseUuid, UUID userUuid) {
        List<UUID> organisationUuids = userLookupService.getUserOrganizations(userUuid);
        for (UUID organisationUuid : organisationUuids) {
            boolean approved = courseTrainingApplicationRepository
                    .existsByCourseUuidAndApplicantTypeAndApplicantUuidAndStatus(
                            courseUuid,
                            CourseTrainingApplicantType.ORGANISATION,
                            organisationUuid,
                            CourseTrainingApplicationStatus.APPROVED);
            if (approved) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the current authenticated user's UUID from the JWT, or null.
     */
    private UUID currentUserUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String keycloakId = getKeycloakId(authentication);
        if (keycloakId == null) {
            return null;
        }
        return userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
    }

    /**
     * Extracts the Keycloak ID from the JWT token.
     *
     * @param authentication The authentication object
     * @return The Keycloak ID (sub claim), or null if not found
     */
    private String getKeycloakId(Authentication authentication) {
        try {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Error extracting Keycloak ID from JWT", e);
        }
        return null;
    }
}
