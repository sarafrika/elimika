package apps.sarafrika.elimika.timetabling.service;

import apps.sarafrika.elimika.course.spi.LessonPerformanceSpi;
import apps.sarafrika.elimika.course.spi.LessonPerformanceSpi.LessonPerformance;
import apps.sarafrika.elimika.course.spi.LessonPerformanceSpi.LessonRef;
import apps.sarafrika.elimika.course.spi.LessonPerformanceSpi.StudentLesson;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.DashboardLinks;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.timetabling.internal.SessionReportLocks;
import apps.sarafrika.elimika.timetabling.model.Enrollment;
import apps.sarafrika.elimika.timetabling.model.ScheduledInstance;
import apps.sarafrika.elimika.timetabling.model.SessionReport;
import apps.sarafrika.elimika.timetabling.model.SessionReportRevision;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import apps.sarafrika.elimika.timetabling.repository.ScheduledInstanceRepository;
import apps.sarafrika.elimika.timetabling.repository.SessionReportRepository;
import apps.sarafrika.elimika.timetabling.repository.SessionReportRevisionRepository;
import apps.sarafrika.elimika.timetabling.spi.ClassReportDTO;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import apps.sarafrika.elimika.timetabling.spi.SessionReportDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportDTO.AggregatesDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportDTO.ChecklistItemDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportDTO.RowDTO;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import apps.sarafrika.elimika.timetabling.spi.SessionReportSummaryDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds, submits and locks the mandatory report for one class session. The live preview is always
 * computed from attendance, the session record and the gradebook; only submissions are stored.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SessionReportService {

    public static final Duration DUE_AFTER_END = Duration.ofHours(24);
    private static final Set<EnrollmentStatus> ROSTER =
            EnumSet.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.ATTENDED, EnrollmentStatus.ABSENT, EnrollmentStatus.EXCUSED);
    private static final String BLOCKER = "blocker";
    private static final String INFO = "info";

    private final ScheduledInstanceRepository instanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionReportRepository reportRepository;
    private final SessionReportRevisionRepository revisionRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final LessonPerformanceSpi lessonPerformance;
    private final OrganisationLookupService organisationLookupService;
    private final TimetableService timetableService;
    private final ObjectMapper objectMapper;
    private final InstructorLookupService instructorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    /** The live report for the session's staff: what it will freeze to if submitted now. */
    @Transactional(readOnly = true)
    public SessionReportDTO preview(UUID instanceUuid) {
        ScheduledInstance instance = instance(instanceUuid);
        SessionReport report = reportRepository.findByScheduledInstanceUuid(instanceUuid).orElse(null);
        SessionReportDTO live = build(instance, report);
        if (report == null || report.getRevisionCount() == null || report.getRevisionCount() == 0) {
            return live;
        }
        return withChanged(live, !sameRows(live, latestSnapshot(report)));
    }

    /** What the school sees: the last submitted revision, or an empty report that only carries its status. */
    @Transactional(readOnly = true)
    public SessionReportDTO submittedReport(UUID instanceUuid) {
        ScheduledInstance instance = instance(instanceUuid);
        SessionReport report = reportRepository.findByScheduledInstanceUuid(instanceUuid).orElse(null);
        if (report != null && report.getRevisionCount() != null && report.getRevisionCount() > 0) {
            SessionReportDTO frozen = objectMapper.convertValue(latestSnapshot(report), SessionReportDTO.class);
            return withStatus(frozen, report);
        }
        SessionReportDTO live = build(instance, report);
        return new SessionReportDTO(live.reportUuid(), live.instanceUuid(), live.classDefinitionUuid(), live.classTitle(),
                live.courseUuid(), live.lessonUuid(), live.lessonNumber(), live.lessonTitle(), live.instructorUuid(),
                live.startTime(), live.endTime(), live.durationMinutes(), live.sessionEnded(), null, null, live.status(),
                live.dueAt(), live.overdue(), null, null, 0, false, live.rubricScored(), live.maxScore(), live.criteria(),
                live.assignmentTitles(), List.of(), null, List.of(), false);
    }

    /** Final submission: refuses an incomplete session, turns complete drafts into scores and freezes a revision. */
    public SessionReportDTO submit(UUID instanceUuid, UUID submittedByUserUuid) {
        ScheduledInstance instance = instance(instanceUuid);
        SessionReport report = ensureReport(instance);
        if (SessionReportLocks.isLocked(report)) {
            throw new IllegalStateException(SessionReportLocks.LOCKED_MESSAGE);
        }
        SessionReportDTO live = build(instance, report);
        List<String> blockers = live.checklist().stream()
                .filter(item -> BLOCKER.equals(item.severity()))
                .map(ChecklistItemDTO::message)
                .toList();
        if (!blockers.isEmpty()) {
            throw new IllegalArgumentException("The session report is not complete: " + String.join("; ", blockers));
        }
        if (instance.getLessonUuid() == null && live.lessonUuid() != null) {
            // Freeze the lesson the session taught: locks match on it, and a later cancellation must not shift it.
            instance.setLessonUuid(live.lessonUuid());
            instance = instanceRepository.save(instance);
        }
        if (live.courseUuid() != null && live.lessonUuid() != null && live.rubricScored()) {
            List<UUID> present = live.rows().stream()
                    .filter(row -> "ATTENDED".equals(row.attendance()))
                    .map(RowDTO::studentUuid)
                    .toList();
            lessonPerformance.submitLessonDrafts(live.courseUuid(), live.lessonUuid(), present, submittedByUserUuid);
        }
        if (instance.getStatus() == SchedulingStatus.ONGOING) {
            timetableService.endScheduledInstance(instanceUuid);
            instance = instance(instanceUuid);
        }

        LocalDateTime now = now();
        int revisionNumber = (report.getRevisionCount() == null ? 0 : report.getRevisionCount()) + 1;
        if (report.getStatus() != SessionReportStatus.SUBMITTED) {
            report.setEditableUntil(now.plusHours(organisationLookupService.sessionReportGraceHours(report.getOrganisationUuid())));
            report.setSubmittedAt(now);
        }
        report.setStatus(SessionReportStatus.SUBMITTED);
        report.setSubmittedByUserUuid(submittedByUserUuid);
        report.setRevisionCount(revisionNumber);
        reportRepository.save(report);

        SessionReportDTO frozen = build(instance, report);
        saveRevision(report, frozen, revisionNumber, submittedByUserUuid, now);
        return frozen;
    }

    /** Platform admin only: opens a locked report again for one more grace window. */
    public SessionReportDTO reopen(UUID instanceUuid) {
        ScheduledInstance instance = instance(instanceUuid);
        SessionReport report = reportRepository.findByScheduledInstanceUuid(instanceUuid)
                .orElseThrow(() -> new IllegalStateException("This session has no submitted report to reopen."));
        if (report.getStatus() == SessionReportStatus.DRAFT) {
            throw new IllegalStateException("This session's report has not been submitted yet.");
        }
        report.setStatus(SessionReportStatus.SUBMITTED);
        report.setEditableUntil(now().plusHours(organisationLookupService.sessionReportGraceHours(report.getOrganisationUuid())));
        reportRepository.save(report);
        return build(instance, report);
    }

    /** Creates the session's report row the first time anyone needs it. */
    public SessionReport ensureReport(ScheduledInstance instance) {
        return reportRepository.findByScheduledInstanceUuid(instance.getUuid()).orElseGet(() -> {
            SessionReport report = new SessionReport();
            report.setScheduledInstanceUuid(instance.getUuid());
            report.setClassDefinitionUuid(instance.getClassDefinitionUuid());
            report.setOrganisationUuid(classDefinitionLookupService.findOrganisationUuid(instance.getClassDefinitionUuid()).orElse(null));
            report.setInstructorUuid(instance.getInstructorUuid());
            report.setStatus(SessionReportStatus.DRAFT);
            report.setDueAt(instance.getEndTime().plus(DUE_AFTER_END));
            report.setRevisionCount(0);
            return reportRepository.save(report);
        });
    }

    /** Opens the report of a session found after its reminder window; the overdue step then flags it. */
    public void trackMissedSession(UUID instanceUuid) {
        ensureReport(instance(instanceUuid));
    }

    /** The "report due" reminder, published once per session however often the job runs. */
    public void remindDue(UUID instanceUuid) {
        ScheduledInstance instance = instance(instanceUuid);
        SessionReport report = ensureReport(instance);
        if (report.getStatus() != SessionReportStatus.DRAFT || reportRepository.claimReminder(report.getUuid(), now()) != 1) {
            return;
        }
        String title = classTitle(instance);
        instructorUserUuid(report).ifPresent(user -> eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                user, "SESSION_REPORT_DUE", "POPUP", "Submit your session report",
                title + " has ended. Submit its session report within 24 hours.",
                DashboardLinks.instructorSessionReport(instance.getClassDefinitionUuid(), instanceUuid),
                Map.of("instanceUuid", instanceUuid.toString(), "dueAt", String.valueOf(report.getDueAt())),
                "session-report-due:" + instanceUuid)));
    }

    /** The overdue notice to the instructor and the school's managers, once per report. */
    public void noticeOverdue(SessionReport report) {
        if (report.getStatus() != SessionReportStatus.DRAFT || reportRepository.claimOverdueNotice(report.getUuid(), now()) != 1) {
            return;
        }
        ScheduledInstance instance = instance(report.getScheduledInstanceUuid());
        String title = classTitle(instance);
        Map<String, Object> metadata = Map.of("instanceUuid", instance.getUuid().toString(), "dueAt", String.valueOf(report.getDueAt()));
        instructorUserUuid(report).ifPresent(user -> eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                user, "SESSION_REPORT_OVERDUE", "POPUP", "Session report overdue",
                "The session report for " + title + " is overdue. Submit it now.",
                DashboardLinks.instructorSessionReport(instance.getClassDefinitionUuid(), instance.getUuid()),
                metadata, "session-report-overdue:" + instance.getUuid())));
        for (UUID manager : organisationLookupService.findManagerUserUuids(report.getOrganisationUuid())) {
            eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                    manager, "SESSION_REPORT_OVERDUE_ORG", "INBOX", "A session report is overdue",
                    "The session report for " + title + " was due 24 hours after the session and has not been submitted.",
                    DashboardLinks.ORGANISATION_SESSION_REPORTS, metadata,
                    "session-report-overdue-org:" + instance.getUuid() + ":" + manager));
        }
    }

    private String classTitle(ScheduledInstance instance) {
        return classDefinitionLookupService.findByUuid(instance.getClassDefinitionUuid())
                .map(ClassDefinitionSnapshot::title)
                .orElse(instance.getTitle() != null ? instance.getTitle() : "Your class");
    }

    private Optional<UUID> instructorUserUuid(SessionReport report) {
        return report.getInstructorUuid() == null ? Optional.empty()
                : instructorLookupService.getInstructorUserUuid(report.getInstructorUuid());
    }

    /** Locks a report whose grace window has passed, freezing any change made since its last submission. */
    public void lock(SessionReport report) {
        if (report.getStatus() != SessionReportStatus.SUBMITTED) {
            return;
        }
        ScheduledInstance instance = instance(report.getScheduledInstanceUuid());
        SessionReportDTO live = build(instance, report);
        if (!sameRows(live, latestSnapshot(report))) {
            int revisionNumber = report.getRevisionCount() + 1;
            report.setRevisionCount(revisionNumber);
            saveRevision(report, live, revisionNumber, report.getSubmittedByUserUuid(), now());
        }
        report.setStatus(SessionReportStatus.LOCKED);
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Page<SessionReportSummaryDTO> listForInstructor(UUID instructorUuid, Collection<SessionReportStatus> statuses,
                                                           Pageable pageable) {
        return reportRepository.findByInstructorUuidAndStatusInOrderByDueAtAsc(instructorUuid, statuses(statuses), pageable)
                .map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<SessionReportSummaryDTO> listForOrganisation(UUID organisationUuid, Collection<SessionReportStatus> statuses,
                                                             Pageable pageable) {
        return reportRepository.findByOrganisationUuidAndStatusInOrderByDueAtAsc(organisationUuid, statuses(statuses), pageable)
                .map(this::summary);
    }

    /**
     * The class across its sessions. The class's own instructor sees live marks; school managers see only
     * what each session's submitted report froze.
     */
    @Transactional(readOnly = true)
    public ClassReportDTO classReport(UUID classDefinitionUuid, boolean live) {
        List<ScheduledInstance> sessions = instanceRepository.findByClassDefinitionUuid(classDefinitionUuid).stream()
                .filter(session -> session.getStatus() != SchedulingStatus.CANCELLED)
                .sorted(Comparator.comparing(ScheduledInstance::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<SessionReportDTO> reports = sessions.stream()
                .map(session -> live ? preview(session.getUuid()) : submittedReport(session.getUuid()))
                .toList();
        List<ClassReportDTO.SessionColumnDTO> columns = reports.stream()
                .map(r -> new ClassReportDTO.SessionColumnDTO(r.instanceUuid(), r.startTime(), r.lessonNumber(), r.lessonTitle(),
                        r.status(), r.overdue(), r.aggregates() != null ? r.aggregates().classAverage() : null, r.maxScore(),
                        r.aggregates() != null ? r.aggregates().attendanceRate() : null))
                .toList();
        List<UUID> students = reports.stream().flatMap(r -> r.rows().stream()).map(RowDTO::studentUuid).distinct().toList();
        List<LessonPerformanceSpi.Band> bands = bandsOf(classDefinitionUuid);
        List<ClassReportDTO.StudentRowDTO> rows = students.stream().map(student -> {
            List<ClassReportDTO.CellDTO> cells = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal maxTotal = BigDecimal.ZERO;
            int scored = 0;
            int attended = 0;
            int absent = 0;
            for (SessionReportDTO r : reports) {
                Optional<RowDTO> row = r.rows().stream().filter(x -> student.equals(x.studentUuid())).findFirst();
                if (row.isEmpty()) {
                    cells.add(new ClassReportDTO.CellDTO(r.instanceUuid(), "NOT_ENROLLED", "none", null));
                    continue;
                }
                RowDTO x = row.get();
                attended += "ATTENDED".equals(x.attendance()) ? 1 : 0;
                absent += "ABSENT".equals(x.attendance()) ? 1 : 0;
                boolean counts = "submitted".equals(x.scoreStatus()) && x.score() != null && x.maxScore() != null;
                if (counts) {
                    total = total.add(x.score());
                    maxTotal = maxTotal.add(x.maxScore());
                    scored++;
                }
                cells.add(new ClassReportDTO.CellDTO(r.instanceUuid(), x.attendance(), x.scoreStatus(), x.score()));
            }
            BigDecimal average = scored == 0 ? null : total.divide(BigDecimal.valueOf(scored), 2, RoundingMode.HALF_UP);
            BigDecimal pct = maxTotal.signum() == 0 ? null
                    : total.multiply(BigDecimal.valueOf(100)).divide(maxTotal, 2, RoundingMode.HALF_UP);
            String band = pct == null ? null : bands.stream()
                    .sorted(Comparator.comparing(LessonPerformanceSpi.Band::minPercentage).reversed())
                    .filter(b -> pct.compareTo(b.minPercentage()) >= 0)
                    .map(LessonPerformanceSpi.Band::name)
                    .findFirst().orElse(null);
            return new ClassReportDTO.StudentRowDTO(student, cells, scored, average, pct, band, attended, absent);
        }).toList();
        String title = classDefinitionLookupService.findByUuid(classDefinitionUuid).map(ClassDefinitionSnapshot::title).orElse(null);
        return new ClassReportDTO(classDefinitionUuid, title, live, columns, rows);
    }

    /** The class report as CSV: one row per learner, one column per session. */
    @Transactional(readOnly = true)
    public String classReportCsv(UUID classDefinitionUuid, boolean live) {
        ClassReportDTO report = classReport(classDefinitionUuid, live);
        StringBuilder csv = new StringBuilder("student_uuid");
        for (ClassReportDTO.SessionColumnDTO column : report.sessions()) {
            csv.append(',').append(csvCell("Lesson " + Objects.toString(column.lessonNumber(), "?") + " "
                    + Objects.toString(column.startTime(), "")));
        }
        csv.append(",average,average_percentage,band,attended,absent\n");
        for (ClassReportDTO.StudentRowDTO row : report.students()) {
            csv.append(row.studentUuid());
            for (ClassReportDTO.CellDTO cell : row.cells()) {
                String value = "ABSENT".equals(cell.attendance()) ? "absent"
                        : cell.score() != null && "submitted".equals(cell.scoreStatus()) ? cell.score().toPlainString()
                        : cell.scoreStatus();
                csv.append(',').append(csvCell(value));
            }
            csv.append(',').append(Objects.toString(row.average(), ""))
                    .append(',').append(Objects.toString(row.averagePercentage(), ""))
                    .append(',').append(csvCell(Objects.toString(row.band(), "")))
                    .append(',').append(row.attended()).append(',').append(row.absent()).append('\n');
        }
        return csv.toString();
    }

    private static String csvCell(String value) {
        String v = value == null ? "" : value;
        return v.contains(",") || v.contains("\"") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    private List<LessonPerformanceSpi.Band> bandsOf(UUID classDefinitionUuid) {
        UUID courseUuid = classDefinitionLookupService.findByUuid(classDefinitionUuid).map(ClassDefinitionSnapshot::courseUuid).orElse(null);
        return courseUuid == null ? List.of() : lessonPerformance.findLessonPerformance(courseUuid, null, List.of()).bands();
    }

    // ===== building the report =====

    private SessionReportDTO build(ScheduledInstance instance, SessionReport report) {
        ClassDefinitionSnapshot classDefinition = classDefinitionLookupService.findByUuid(instance.getClassDefinitionUuid()).orElse(null);
        UUID courseUuid = classDefinition != null ? classDefinition.courseUuid() : null;
        Optional<LessonRef> lesson = courseUuid == null ? Optional.empty()
                : lessonPerformance.resolveLesson(courseUuid, instance.getLessonUuid(), sessionNumber(instance));
        List<Enrollment> roster = enrollmentRepository.findByScheduledInstanceUuid(instance.getUuid()).stream()
                .filter(enrollment -> ROSTER.contains(enrollment.getStatus()))
                .toList();
        List<UUID> students = roster.stream().map(Enrollment::getStudentUuid).distinct().toList();
        LessonPerformance performance = lesson
                .map(ref -> lessonPerformance.findLessonPerformance(courseUuid, ref.lessonUuid(), students))
                .orElse(null);
        Map<UUID, StudentLesson> byStudent = new LinkedHashMap<>();
        if (performance != null) {
            performance.students().forEach(s -> byStudent.put(s.studentUuid(), s));
        }
        boolean rubricScored = performance != null && performance.rubricScored();
        int criteriaCount = performance == null ? 0 : performance.criteria().size();

        List<RowDTO> rows = roster.stream()
                .map(enrollment -> row(enrollment, byStudent.get(enrollment.getStudentUuid())))
                .toList();
        LocalDateTime now = now();
        boolean ended = instance.getStatus() == SchedulingStatus.COMPLETED
                || (instance.getEndTime() != null && !instance.getEndTime().isAfter(now));
        List<ChecklistItemDTO> checklist = checklist(instance, rows, rubricScored, criteriaCount, ended);

        SessionReportStatus status = report == null ? SessionReportStatus.DRAFT
                : SessionReportLocks.isLocked(report) ? SessionReportStatus.LOCKED : report.getStatus();
        LocalDateTime dueAt = report != null ? report.getDueAt()
                : instance.getEndTime() != null ? instance.getEndTime().plus(DUE_AFTER_END) : null;
        boolean overdue = status == SessionReportStatus.DRAFT && dueAt != null && dueAt.isBefore(now);
        boolean canSubmit = status != SessionReportStatus.LOCKED
                && checklist.stream().noneMatch(item -> BLOCKER.equals(item.severity()));

        return new SessionReportDTO(
                report != null ? report.getUuid() : null,
                instance.getUuid(),
                instance.getClassDefinitionUuid(),
                classDefinition != null ? classDefinition.title() : instance.getTitle(),
                courseUuid,
                lesson.map(LessonRef::lessonUuid).orElse(null),
                lesson.map(LessonRef::lessonNumber).orElse(null),
                lesson.map(LessonRef::title).orElse(null),
                instance.getInstructorUuid(),
                instance.getStartTime(),
                instance.getEndTime(),
                instance.getStartTime() != null && instance.getEndTime() != null
                        ? Duration.between(instance.getStartTime(), instance.getEndTime()).toMinutes() : null,
                ended,
                instance.getTopicCovered(),
                instance.getInstructorRemarks(),
                status,
                dueAt,
                overdue,
                report != null ? report.getSubmittedAt() : null,
                report != null ? report.getEditableUntil() : null,
                report != null && report.getRevisionCount() != null ? report.getRevisionCount() : 0,
                false,
                rubricScored,
                performance != null ? performance.maxScore() : null,
                performance == null ? List.of() : performance.criteria().stream()
                        .map(c -> new SessionReportDTO.CriterionDTO(c.criteriaUuid(), c.name()))
                        .toList(),
                performance == null ? List.of() : performance.assignmentTitles(),
                rows,
                aggregates(rows, performance),
                checklist,
                canSubmit);
    }

    private static RowDTO row(Enrollment enrollment, StudentLesson lesson) {
        String attendance = enrollment.getStatus() == EnrollmentStatus.ATTENDED ? "ATTENDED"
                : enrollment.getStatus() == EnrollmentStatus.ABSENT ? "ABSENT"
                : enrollment.getStatus() == EnrollmentStatus.EXCUSED ? "EXCUSED" : "NOT_MARKED";
        List<SessionReportDTO.PickDTO> picks = lesson == null ? List.of() : lesson.picks().stream()
                .map(p -> new SessionReportDTO.PickDTO(p.criteriaUuid(), p.criteriaName(), p.levelName(), p.points()))
                .toList();
        BigDecimal score = lesson == null ? null : lesson.score() != null ? lesson.score()
                : "draft".equals(lesson.status()) ? picks.stream().map(SessionReportDTO.PickDTO::points)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add) : null;
        return new RowDTO(
                enrollment.getStudentUuid(),
                enrollment.getUuid(),
                lesson != null ? lesson.enrollmentUuid() : null,
                attendance,
                enrollment.getInstrumentCondition() != null ? enrollment.getInstrumentCondition().getValue() : null,
                enrollment.getAttendanceComment(),
                lesson != null ? lesson.status() : "none",
                score,
                lesson != null ? lesson.maxScore() : null,
                picks,
                lesson != null ? lesson.comments() : null,
                lesson != null ? lesson.revisionCount() : 0,
                lesson == null ? List.of() : lesson.assignments().stream()
                        .map(a -> new SessionReportDTO.AssignmentDTO(a.title(), a.status(), a.score(), a.maxScore()))
                        .toList());
    }

    private static List<ChecklistItemDTO> checklist(ScheduledInstance instance, List<RowDTO> rows, boolean rubricScored,
                                                    int criteriaCount, boolean ended) {
        List<ChecklistItemDTO> items = new ArrayList<>();
        if (!ended) {
            items.add(new ChecklistItemDTO("SESSION_NOT_ENDED", BLOCKER, null, "The session has not ended yet"));
        }
        if (instance.getTopicCovered() == null || instance.getTopicCovered().isBlank()) {
            items.add(new ChecklistItemDTO("TOPIC_NOT_RECORDED", BLOCKER, null, "Record what was taught in this session"));
        }
        for (RowDTO row : rows) {
            if ("NOT_MARKED".equals(row.attendance())) {
                items.add(new ChecklistItemDTO("ATTENDANCE_NOT_MARKED", BLOCKER, row.studentUuid(), "Mark attendance"));
                continue;
            }
            if (!rubricScored || !"ATTENDED".equals(row.attendance())) {
                continue;
            }
            if ("none".equals(row.scoreStatus())) {
                items.add(new ChecklistItemDTO("SCORE_MISSING", BLOCKER, row.studentUuid(), "Score this lesson"));
            } else if ("draft".equals(row.scoreStatus()) && row.picks().size() < criteriaCount) {
                items.add(new ChecklistItemDTO("SCORE_INCOMPLETE_DRAFT", BLOCKER, row.studentUuid(),
                        "Finish the draft score: " + row.picks().size() + " of " + criteriaCount + " criteria scored"));
            } else if ("draft".equals(row.scoreStatus())) {
                items.add(new ChecklistItemDTO("DRAFT_WILL_BE_SUBMITTED", INFO, row.studentUuid(),
                        "This draft score will be submitted with the report"));
            }
        }
        return items;
    }

    private static AggregatesDTO aggregates(List<RowDTO> rows, LessonPerformance performance) {
        int present = (int) rows.stream().filter(r -> "ATTENDED".equals(r.attendance())).count();
        int absent = (int) rows.stream().filter(r -> "ABSENT".equals(r.attendance())).count();
        int excused = (int) rows.stream().filter(r -> "EXCUSED".equals(r.attendance())).count();
        int notMarked = rows.size() - present - absent - excused;
        BigDecimal rate = present + absent == 0 ? null
                : BigDecimal.valueOf(present * 100L).divide(BigDecimal.valueOf(present + absent), 2, RoundingMode.HALF_UP);
        int criteriaCount = performance == null ? 0 : performance.criteria().size();
        List<RowDTO> scored = rows.stream()
                .filter(r -> r.score() != null && ("submitted".equals(r.scoreStatus())
                        || ("draft".equals(r.scoreStatus()) && r.picks().size() == criteriaCount)))
                .toList();
        BigDecimal max = performance != null ? performance.maxScore() : null;
        BigDecimal average = scored.isEmpty() ? null : scored.stream().map(RowDTO::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(scored.size()), 2, RoundingMode.HALF_UP);
        BigDecimal averagePct = average == null || max == null || max.signum() == 0 ? null
                : average.multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP);
        List<SessionReportDTO.CriterionAverageDTO> criterionAverages = performance == null ? List.of()
                : performance.criteria().stream().map(c -> {
                    List<BigDecimal> points = scored.stream()
                            .flatMap(r -> r.picks().stream())
                            .filter(p -> c.criteriaUuid().equals(p.criteriaUuid()) && p.points() != null)
                            .map(SessionReportDTO.PickDTO::points)
                            .toList();
                    BigDecimal avg = points.isEmpty() ? null : points.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
                    return new SessionReportDTO.CriterionAverageDTO(c.criteriaUuid(), c.name(), avg);
                }).toList();
        List<SessionReportDTO.BandCountDTO> bandCounts = new ArrayList<>();
        if (performance != null && max != null && max.signum() > 0) {
            List<LessonPerformanceSpi.Band> bands = performance.bands().stream()
                    .sorted(Comparator.comparing(LessonPerformanceSpi.Band::minPercentage).reversed())
                    .toList();
            for (LessonPerformanceSpi.Band band : bands) {
                int count = (int) scored.stream()
                        .map(r -> r.score().multiply(BigDecimal.valueOf(100)).divide(max, 2, RoundingMode.HALF_UP))
                        .filter(pct -> bands.stream().filter(b -> pct.compareTo(b.minPercentage()) >= 0).findFirst()
                                .map(b -> b.name().equals(band.name())).orElse(false))
                        .count();
                bandCounts.add(new SessionReportDTO.BandCountDTO(band.name(), count));
            }
        }
        return new AggregatesDTO(rows.size(), present, absent, notMarked, rate, scored.size(), average, averagePct,
                scored.stream().map(RowDTO::score).max(Comparator.naturalOrder()).orElse(null),
                scored.stream().map(RowDTO::score).min(Comparator.naturalOrder()).orElse(null),
                criterionAverages, bandCounts);
    }

    // ===== helpers =====

    private void saveRevision(SessionReport report, SessionReportDTO frozen, int revisionNumber, UUID by, LocalDateTime at) {
        SessionReportRevision revision = new SessionReportRevision();
        revision.setSessionReportUuid(report.getUuid());
        revision.setRevisionNumber(revisionNumber);
        revision.setSnapshot(objectMapper.valueToTree(frozen));
        revision.setSubmittedByUserUuid(by);
        revision.setSubmittedAt(at);
        revisionRepository.save(revision);
    }

    private JsonNode latestSnapshot(SessionReport report) {
        return revisionRepository.findFirstBySessionReportUuidOrderByRevisionNumberDesc(report.getUuid())
                .map(SessionReportRevision::getSnapshot)
                .orElse(null);
    }

    /** Compares what learners would see: rows, the session record and nothing about timing. */
    private boolean sameRows(SessionReportDTO live, JsonNode snapshot) {
        if (snapshot == null) {
            return false;
        }
        JsonNode current = objectMapper.valueToTree(live);
        return current.path("rows").equals(snapshot.path("rows"))
                && current.path("topic_covered").equals(snapshot.path("topic_covered"))
                && current.path("instructor_remarks").equals(snapshot.path("instructor_remarks"));
    }

    private SessionReportDTO withChanged(SessionReportDTO r, boolean changed) {
        return new SessionReportDTO(r.reportUuid(), r.instanceUuid(), r.classDefinitionUuid(), r.classTitle(), r.courseUuid(),
                r.lessonUuid(), r.lessonNumber(), r.lessonTitle(), r.instructorUuid(), r.startTime(), r.endTime(),
                r.durationMinutes(), r.sessionEnded(), r.topicCovered(), r.instructorRemarks(), r.status(), r.dueAt(),
                r.overdue(), r.submittedAt(), r.editableUntil(), r.revisionCount(), changed, r.rubricScored(), r.maxScore(),
                r.criteria(), r.assignmentTitles(), r.rows(), r.aggregates(), r.checklist(), r.canSubmit());
    }

    /** A frozen snapshot shown with the report's current status and lock time. */
    private SessionReportDTO withStatus(SessionReportDTO r, SessionReport report) {
        SessionReportStatus status = SessionReportLocks.isLocked(report) ? SessionReportStatus.LOCKED : report.getStatus();
        return new SessionReportDTO(r.reportUuid(), r.instanceUuid(), r.classDefinitionUuid(), r.classTitle(), r.courseUuid(),
                r.lessonUuid(), r.lessonNumber(), r.lessonTitle(), r.instructorUuid(), r.startTime(), r.endTime(),
                r.durationMinutes(), r.sessionEnded(), r.topicCovered(), r.instructorRemarks(), status, report.getDueAt(),
                false, report.getSubmittedAt(), report.getEditableUntil(), report.getRevisionCount(), false,
                r.rubricScored(), r.maxScore(), r.criteria(), r.assignmentTitles(), r.rows(), r.aggregates(), List.of(), false);
    }

    private SessionReportSummaryDTO summary(SessionReport report) {
        ScheduledInstance instance = instanceRepository.findByUuid(report.getScheduledInstanceUuid()).orElse(null);
        SessionReportStatus status = SessionReportLocks.isLocked(report) ? SessionReportStatus.LOCKED : report.getStatus();
        LocalDateTime now = now();
        return new SessionReportSummaryDTO(report.getUuid(), report.getScheduledInstanceUuid(), report.getClassDefinitionUuid(),
                report.getInstructorUuid(), instance != null ? instance.getStartTime() : null,
                instance != null ? instance.getEndTime() : null, status, report.getDueAt(),
                status == SessionReportStatus.DRAFT && report.getDueAt() != null && report.getDueAt().isBefore(now),
                report.getSubmittedAt(),
                report.getSubmittedAt() != null && report.getDueAt() != null && report.getSubmittedAt().isAfter(report.getDueAt()),
                report.getEditableUntil(), report.getRevisionCount() == null ? 0 : report.getRevisionCount());
    }

    private static Collection<SessionReportStatus> statuses(Collection<SessionReportStatus> requested) {
        return requested == null || requested.isEmpty() ? EnumSet.allOf(SessionReportStatus.class) : requested;
    }

    private int sessionNumber(ScheduledInstance instance) {
        List<ScheduledInstance> sessions = instanceRepository.findByClassDefinitionUuid(instance.getClassDefinitionUuid()).stream()
                .filter(session -> session.getStatus() != SchedulingStatus.CANCELLED)
                .sorted(Comparator.comparing(ScheduledInstance::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getUuid().equals(instance.getUuid())) {
                return i + 1;
            }
        }
        return sessions.size() + 1;
    }

    private ScheduledInstance instance(UUID instanceUuid) {
        return instanceRepository.findByUuid(instanceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled instance " + instanceUuid + " not found"));
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
