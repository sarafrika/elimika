package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ClassProgrammeProgressDTO;
import apps.sarafrika.elimika.course.dto.ProgramRemarkDTO;
import apps.sarafrika.elimika.course.dto.ProgramReportDTO;
import apps.sarafrika.elimika.course.dto.ProgramReportSummaryDTO;
import apps.sarafrika.elimika.course.dto.ProgramTargetSkillDTO;
import apps.sarafrika.elimika.course.internal.GradeBands;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.LearnerSkillEvidence;
import apps.sarafrika.elimika.course.model.LearnerSkillLevel;
import apps.sarafrika.elimika.course.model.ProgramCourse;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.model.ProgramEnrollmentRemark;
import apps.sarafrika.elimika.course.model.TrainingProgram;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseGradeBandRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillEvidenceRepository;
import apps.sarafrika.elimika.course.repository.LearnerSkillLevelRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRemarkRepository;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.TrainingProgramRepository;
import apps.sarafrika.elimika.course.service.ProgramReportService;
import apps.sarafrika.elimika.course.service.ProgramSkillService;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramReportServiceImpl implements ProgramReportService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final TrainingProgramRepository programRepository;
    private final ProgramEnrollmentRepository programEnrollmentRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseGradeBandRepository gradeBandRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final LearnerSkillLevelRepository levelRepository;
    private final LearnerSkillEvidenceRepository evidenceRepository;
    private final ProgramEnrollmentRemarkRepository remarkRepository;
    private final ProgramSkillService programSkillService;
    private final EnrollmentLookupService enrollmentLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @Override
    public List<ProgramReportSummaryDTO> listForStudent(UUID studentUuid) {
        Set<UUID> seen = new java.util.HashSet<>();
        return programEnrollmentRepository.findByStudentUuidOrderByCreatedDateDesc(studentUuid).stream()
                .filter(enrollment -> seen.add(enrollment.getProgramUuid()))
                .map(enrollment -> new ProgramReportSummaryDTO(enrollment.getProgramUuid(),
                        programRepository.findByUuid(enrollment.getProgramUuid()).map(TrainingProgram::getTitle).orElse(null),
                        enrollment.getUuid(), enrollment.getFinalGrade(),
                        !Boolean.FALSE.equals(enrollment.getFinalGradeProvisional()),
                        enrollment.getResultStatus() == null ? CourseResultStatus.IN_PROGRESS.name() : enrollment.getResultStatus().name()))
                .toList();
    }

    @Override
    public ProgramReportDTO getReport(UUID programUuid, UUID studentUuid, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The period ends before it starts");
        }
        TrainingProgram program = program(programUuid);
        ProgramEnrollment enrollment = enrollment(programUuid, studentUuid);
        List<ProgramReportDTO.CourseRow> courses = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid)
                .stream().map(member -> courseRow(member, studentUuid, from, to)).toList();
        List<ProgramReportDTO.CourseRow> required = courses.stream().filter(ProgramReportDTO.CourseRow::isRequired).toList();
        int passed = (int) required.stream().filter(row -> CourseResultStatus.PASSED.name().equals(row.resultStatus())).count();
        return new ProgramReportDTO(programUuid, program.getTitle(), program.getProgramCode(), enrollment.getUuid(), studentUuid,
                enrollment.getFinalGrade(), !Boolean.FALSE.equals(enrollment.getFinalGradeProvisional()), program.getPassMark(),
                enrollment.getResultStatus() == null ? CourseResultStatus.IN_PROGRESS.name() : enrollment.getResultStatus().name(),
                required.size(), passed, from, to, courses, skillRows(programUuid, studentUuid), getRemarks(programUuid, studentUuid));
    }

    @Override
    public List<ProgramRemarkDTO> getRemarks(UUID programUuid, UUID studentUuid) {
        ProgramEnrollment enrollment = enrollment(programUuid, studentUuid);
        return remarkRepository.findByProgramEnrollmentUuidOrderByCreatedDateDescIdDesc(enrollment.getUuid()).stream()
                .map(ProgramRemarkDTO::from).toList();
    }

    @Override
    @Transactional
    public ProgramRemarkDTO addRemark(UUID programUuid, UUID studentUuid, ProgramRemarkDTO remark, UUID authorUserUuid) {
        ProgramEnrollment enrollment = enrollment(programUuid, studentUuid);
        String strengths = trimmed(remark.strengths());
        String improvements = trimmed(remark.improvements());
        String recommendations = trimmed(remark.recommendations());
        if (strengths == null && improvements == null && recommendations == null) {
            throw new IllegalArgumentException("Write at least one of strengths, areas to improve or recommendations");
        }
        ProgramEnrollmentRemark saved = new ProgramEnrollmentRemark();
        saved.setProgramEnrollmentUuid(enrollment.getUuid());
        saved.setStrengths(strengths);
        saved.setImprovements(improvements);
        saved.setRecommendations(recommendations);
        saved.setNextReviewDate(remark.nextReviewDate());
        saved.setAuthorUserUuid(authorUserUuid);
        return ProgramRemarkDTO.from(remarkRepository.save(saved));
    }

    @Override
    public ClassProgrammeProgressDTO getClassProgress(UUID classDefinitionUuid, UUID programUuid) {
        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot = classDefinitionLookupService.findByUuid(classDefinitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classDefinitionUuid));
        UUID resolved = resolveProgramme(snapshot, programUuid);
        TrainingProgram program = program(resolved);
        List<ProgramTargetSkillDTO> targets = programSkillService.getSkills(resolved);
        List<ProgramCourse> required = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(resolved).stream()
                .filter(member -> !Boolean.FALSE.equals(member.getIsRequired())).toList();

        Set<UUID> students = enrollmentLookupService.findStudentUuidsOfClassDefinition(classDefinitionUuid);
        Map<UUID, List<LearnerSkillLevel>> levelsByStudent = students.isEmpty() ? Map.of()
                : levelRepository.findByProgramUuidAndStudentUuidIn(resolved, students).stream()
                .collect(Collectors.groupingBy(LearnerSkillLevel::getStudentUuid));

        List<ClassProgrammeProgressDTO.Learner> learners = new ArrayList<>();
        for (UUID student : students) {
            Optional<ProgramEnrollment> enrollment = programEnrollmentRepository
                    .findFirstByStudentUuidAndProgramUuidOrderByCreatedDateDesc(student, resolved);
            if (enrollment.isEmpty()) {
                continue;
            }
            int passed = (int) required.stream()
                    .filter(member -> courseEnrollmentRepository.findByStudentUuidAndCourseUuid(student, member.getCourseUuid())
                            .map(CourseEnrollment::getResultStatus).orElse(null) == CourseResultStatus.PASSED)
                    .count();
            Map<UUID, LearnerSkillLevel> levels = levelsByStudent.getOrDefault(student, List.of()).stream()
                    .collect(Collectors.toMap(LearnerSkillLevel::getSkillUuid, level -> level));
            List<ClassProgrammeProgressDTO.SkillLevel> skillLevels = targets.stream()
                    .map(target -> Optional.ofNullable(levels.get(target.skillUuid()))
                            .map(level -> new ClassProgrammeProgressDTO.SkillLevel(target.skillUuid(), level.getLevelName(),
                                    Boolean.TRUE.equals(level.getCountsAsAchieved())))
                            .orElse(new ClassProgrammeProgressDTO.SkillLevel(target.skillUuid(), null, false)))
                    .toList();
            ProgramEnrollment found = enrollment.get();
            learners.add(new ClassProgrammeProgressDTO.Learner(student, found.getUuid(), found.getFinalGrade(),
                    !Boolean.FALSE.equals(found.getFinalGradeProvisional()),
                    found.getResultStatus() == null ? CourseResultStatus.IN_PROGRESS.name() : found.getResultStatus().name(),
                    required.size(), passed, skillLevels));
        }

        List<ClassProgrammeProgressDTO.SkillSummary> skills = targets.stream().map(target -> {
            int achieved = 0;
            int developing = 0;
            int notAchieved = 0;
            int none = 0;
            for (ClassProgrammeProgressDTO.Learner learner : learners) {
                LearnerSkillLevel level = levelsByStudent.getOrDefault(learner.studentUuid(), List.of()).stream()
                        .filter(found -> found.getSkillUuid().equals(target.skillUuid())).findFirst().orElse(null);
                if (level == null) {
                    none++;
                } else if (Boolean.TRUE.equals(level.getCountsAsAchieved())) {
                    achieved++;
                } else if (level.getLevelRank() != null && level.getLevelRank() > 1) {
                    developing++;
                } else {
                    notAchieved++;
                }
            }
            return new ClassProgrammeProgressDTO.SkillSummary(target.skillUuid(), target.skillName(), achieved, developing, notAchieved, none);
        }).toList();
        return new ClassProgrammeProgressDTO(classDefinitionUuid, resolved, program.getTitle(), skills, learners);
    }

    private ProgramReportDTO.CourseRow courseRow(ProgramCourse member, UUID studentUuid, LocalDate from, LocalDate to) {
        Course course = courseRepository.findByUuid(member.getCourseUuid()).orElse(null);
        CourseEnrollment courseEnrollment = courseEnrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, member.getCourseUuid())
                .orElse(null);
        boolean required = !Boolean.FALSE.equals(member.getIsRequired());
        BigDecimal finalGrade = courseEnrollment == null ? null : courseEnrollment.getFinalGrade();
        String result = courseEnrollment == null ? "NOT_STARTED"
                : courseEnrollment.getResultStatus() == null ? CourseResultStatus.IN_PROGRESS.name() : courseEnrollment.getResultStatus().name();
        boolean decided = CourseResultStatus.PASSED.name().equals(result) || CourseResultStatus.FAILED.name().equals(result);
        BigDecimal contribution = required && decided && finalGrade != null && member.getWeightPercentage() != null
                ? finalGrade.multiply(member.getWeightPercentage()).divide(HUNDRED, 2, RoundingMode.HALF_UP) : null;
        String band = finalGrade == null ? null
                : GradeBands.labelFor(gradeBandRepository.findByCourseUuidOrderByMinPercentageDesc(member.getCourseUuid()), finalGrade);

        int attended = 0;
        int absent = 0;
        int excused = 0;
        for (EnrollmentLookupService.LearnerSessionSnapshot session
                : enrollmentLookupService.findSessionsOfStudentForCourse(studentUuid, member.getCourseUuid())) {
            if (!inPeriod(session.startTime(), from, to) || session.attendanceStatus() == null) {
                continue;
            }
            switch (session.attendanceStatus().toUpperCase(Locale.ROOT)) {
                case "ATTENDED" -> attended++;
                case "ABSENT" -> absent++;
                case "EXCUSED" -> excused++;
                default -> { }
            }
        }
        return new ProgramReportDTO.CourseRow(member.getCourseUuid(), course == null ? null : course.getName(),
                course == null ? null : course.getCourseCode(), required, required ? member.getWeightPercentage() : null,
                courseEnrollment == null ? null : courseEnrollment.getUuid(), finalGrade, band, result, contribution,
                attended, attended + absent, excused);
    }

    private List<ProgramReportDTO.SkillRow> skillRows(UUID programUuid, UUID studentUuid) {
        Map<UUID, LearnerSkillLevel> levels = levelRepository.findByStudentUuidAndProgramUuid(studentUuid, programUuid).stream()
                .collect(Collectors.toMap(LearnerSkillLevel::getSkillUuid, level -> level));
        Map<UUID, List<LearnerSkillEvidence>> evidence = evidenceRepository
                .findByStudentUuidAndProgramUuidOrderByGradedAtDescIdDesc(studentUuid, programUuid).stream()
                .collect(Collectors.groupingBy(LearnerSkillEvidence::getSkillUuid));
        Map<UUID, String> titles = new HashMap<>();
        Map<UUID, UUID> courses = new HashMap<>();
        return programSkillService.getSkills(programUuid).stream().map(target -> {
            LearnerSkillLevel level = levels.get(target.skillUuid());
            List<ProgramReportDTO.Evidence> history = evidence.getOrDefault(target.skillUuid(), List.of()).stream()
                    .map(item -> new ProgramReportDTO.Evidence(
                            courses.computeIfAbsent(item.getCourseAssessmentUuid(), this::courseOfAssessment),
                            item.getCourseAssessmentUuid(),
                            titles.computeIfAbsent(item.getLineItemUuid(), lineItem -> sourceTitle(item.getCourseAssessmentUuid(), lineItem)),
                            item.getScore(), item.getLevelName(), item.getGradedAt(), Boolean.TRUE.equals(item.getSuperseded())))
                    .toList();
            return new ProgramReportDTO.SkillRow(target.skillUuid(), target.skillName(),
                    level == null ? null : level.getLevelName(), level != null && Boolean.TRUE.equals(level.getCountsAsAchieved()),
                    level == null ? null : level.getScore(), level == null ? null : level.getGradedAt(), history);
        }).toList();
    }

    private UUID courseOfAssessment(UUID assessmentUuid) {
        return courseAssessmentRepository.findByUuid(assessmentUuid).map(CourseAssessment::getCourseUuid).orElse(null);
    }

    private String sourceTitle(UUID assessmentUuid, UUID lineItemUuid) {
        String assessment = courseAssessmentRepository.findByUuid(assessmentUuid).map(CourseAssessment::getTitle).orElse(null);
        String lineItem = lineItemRepository.findByUuid(lineItemUuid).map(CourseAssessmentLineItem::getTitle).orElse(null);
        if (assessment == null || lineItem == null || assessment.equals(lineItem)) {
            return assessment != null ? assessment : lineItem;
        }
        return assessment + " · " + lineItem;
    }

    private UUID resolveProgramme(ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot, UUID requested) {
        if (requested != null) {
            return requested;
        }
        if (snapshot.programUuid() != null) {
            return snapshot.programUuid();
        }
        List<UUID> programmes = snapshot.courseUuid() == null ? List.of()
                : programCourseRepository.findByCourseUuid(snapshot.courseUuid()).stream().map(ProgramCourse::getProgramUuid).distinct().toList();
        if (programmes.size() != 1) {
            throw new IllegalArgumentException(programmes.isEmpty()
                    ? "This class's course is not part of a programme"
                    : "This class's course is in several programmes; pass program_uuid");
        }
        return programmes.getFirst();
    }

    private TrainingProgram program(UUID programUuid) {
        return programRepository.findByUuid(programUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found: " + programUuid));
    }

    private ProgramEnrollment enrollment(UUID programUuid, UUID studentUuid) {
        return programEnrollmentRepository.findFirstByStudentUuidAndProgramUuidOrderByCreatedDateDesc(studentUuid, programUuid)
                .orElseThrow(() -> new ResourceNotFoundException("The learner is not enrolled in this programme"));
    }

    private static boolean inPeriod(LocalDateTime start, LocalDate from, LocalDate to) {
        if (start == null) {
            return from == null && to == null;
        }
        return (from == null || !start.toLocalDate().isBefore(from)) && (to == null || !start.toLocalDate().isAfter(to));
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
