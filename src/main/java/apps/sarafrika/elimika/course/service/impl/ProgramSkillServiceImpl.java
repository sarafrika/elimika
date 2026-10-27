package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.ProgramSkillMappingDTO;
import apps.sarafrika.elimika.course.dto.ProgramTargetSkillDTO;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.ProgramSkillMapping;
import apps.sarafrika.elimika.course.model.ProgramTargetSkill;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramCourseRepository;
import apps.sarafrika.elimika.course.repository.ProgramSkillMappingRepository;
import apps.sarafrika.elimika.course.repository.ProgramTargetSkillRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.service.ProgramSkillService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramSkillServiceImpl implements ProgramSkillService {

    private static final BigDecimal FULL_SHARE = new BigDecimal("100");

    private final ProgramTargetSkillRepository targetSkillRepository;
    private final ProgramSkillMappingRepository mappingRepository;
    private final ProgramCourseRepository programCourseRepository;
    private final CourseRepository courseRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final RubricCriteriaRepository criteriaRepository;
    private final SkillLookupService skillLookupService;

    @Override
    @Transactional(readOnly = true)
    public List<ProgramTargetSkillDTO> getSkills(UUID programUuid) {
        List<ProgramTargetSkill> targets = targetSkillRepository.findByProgramUuidOrderByDisplayOrderAsc(programUuid);
        Map<UUID, String> names = skillLookupService.findByUuids(targets.stream().map(ProgramTargetSkill::getSkillUuid).toList())
                .stream().collect(Collectors.toMap(SkillSummary::uuid, SkillSummary::name));
        Map<UUID, List<ProgramSkillMapping>> bySkill = mappingRepository.findByProgramUuidAndActiveTrue(programUuid).stream()
                .collect(Collectors.groupingBy(ProgramSkillMapping::getSkillUuid));
        return targets.stream()
                .map(target -> {
                    List<ProgramSkillMappingDTO> mappings = bySkill.getOrDefault(target.getSkillUuid(), List.of()).stream()
                            .map(this::toDTO).toList();
                    return new ProgramTargetSkillDTO(target.getSkillUuid(), names.get(target.getSkillUuid()),
                            target.getDisplayOrder(), !mappings.isEmpty(), mappings);
                })
                .toList();
    }

    @Override
    public List<ProgramTargetSkillDTO> replaceTargetSkills(UUID programUuid, List<UUID> skillUuids) {
        List<UUID> ordered = new ArrayList<>(new LinkedHashSet<>(skillUuids));
        Set<UUID> known = skillLookupService.findByUuids(ordered).stream().map(SkillSummary::uuid).collect(Collectors.toSet());
        List<UUID> unknown = ordered.stream().filter(uuid -> !known.contains(uuid)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown skills: " + unknown);
        }
        if (ordered.isEmpty()) {
            mappingRepository.deleteByProgramUuid(programUuid);
        } else {
            mappingRepository.deleteByProgramUuidAndSkillUuidNotIn(programUuid, ordered);
        }
        targetSkillRepository.deleteByProgramUuid(programUuid);
        targetSkillRepository.flush();
        List<ProgramTargetSkill> targets = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            ProgramTargetSkill target = new ProgramTargetSkill();
            target.setProgramUuid(programUuid);
            target.setSkillUuid(ordered.get(i));
            target.setDisplayOrder(i + 1);
            targets.add(target);
        }
        targetSkillRepository.saveAll(targets);
        return getSkills(programUuid);
    }

    @Override
    public ProgramTargetSkillDTO replaceMappings(UUID programUuid, UUID skillUuid, List<ProgramSkillMappingDTO> mappings) {
        boolean target = targetSkillRepository.findByProgramUuidOrderByDisplayOrderAsc(programUuid).stream()
                .anyMatch(skill -> skill.getSkillUuid().equals(skillUuid));
        if (!target) {
            throw new IllegalArgumentException("Add the skill to the programme's target skills before linking evidence");
        }
        validate(programUuid, mappings);
        mappingRepository.deleteByProgramUuidAndSkillUuid(programUuid, skillUuid);
        mappingRepository.flush();
        mappingRepository.saveAll(mappings.stream().map(dto -> {
            ProgramSkillMapping mapping = new ProgramSkillMapping();
            mapping.setProgramUuid(programUuid);
            mapping.setSkillUuid(skillUuid);
            mapping.setCourseAssessmentUuid(dto.courseAssessmentUuid());
            mapping.setRubricCriteriaUuid(dto.rubricCriteriaUuid());
            mapping.setSharePercentage(dto.sharePercentage());
            mapping.setActive(Boolean.TRUE);
            return mapping;
        }).toList());
        return getSkills(programUuid).stream().filter(skill -> skill.skillUuid().equals(skillUuid)).findFirst().orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceSkillsMapped(UUID programUuid) {
        List<String> unmapped = getSkills(programUuid).stream()
                .filter(skill -> !skill.ready())
                .map(skill -> Objects.requireNonNullElse(skill.skillName(), skill.skillUuid().toString()))
                .toList();
        if (!unmapped.isEmpty()) {
            throw new IllegalStateException("Link graded evidence to every target skill before publishing: " + String.join(", ", unmapped));
        }
    }

    /** Assessments belong to programme courses, criteria to their rubrics, and shares total 100 per assessment. */
    private void validate(UUID programUuid, List<ProgramSkillMappingDTO> mappings) {
        Set<UUID> programCourses = programCourseRepository.findByProgramUuidOrderBySequenceOrderAsc(programUuid).stream()
                .map(member -> member.getCourseUuid()).collect(Collectors.toSet());
        Map<UUID, BigDecimal> shares = new HashMap<>();
        Map<UUID, Set<String>> sources = new HashMap<>();
        Map<UUID, Boolean> wholeScore = new HashMap<>();
        for (ProgramSkillMappingDTO dto : mappings) {
            CourseAssessment assessment = courseAssessmentRepository.findByUuid(dto.courseAssessmentUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Course assessment not found: " + dto.courseAssessmentUuid()));
            if (!programCourses.contains(liveCourseUuid(assessment.getCourseUuid()))) {
                throw new IllegalArgumentException("\"" + assessment.getTitle() + "\" belongs to a course outside this programme");
            }
            if (dto.rubricCriteriaUuid() != null) {
                RubricCriteria criterion = criteriaRepository.findByUuid(dto.rubricCriteriaUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Rubric criterion not found: " + dto.rubricCriteriaUuid()));
                if (!rubricsOf(assessment).contains(criterion.getRubricUuid())) {
                    throw new IllegalArgumentException("\"" + criterion.getComponentName() + "\" is not a criterion of \"" + assessment.getTitle() + "\"'s rubric");
                }
            }
            String source = String.valueOf(dto.rubricCriteriaUuid());
            if (!sources.computeIfAbsent(assessment.getUuid(), key -> new HashSet<>()).add(source)) {
                throw new IllegalArgumentException("Each criterion may be linked only once per assessment");
            }
            boolean whole = dto.rubricCriteriaUuid() == null;
            Boolean previous = wholeScore.putIfAbsent(assessment.getUuid(), whole);
            if (previous != null && (previous || whole)) {
                throw new IllegalArgumentException("Link \"" + assessment.getTitle() + "\" either as a whole score or by criteria, not both");
            }
            shares.merge(assessment.getUuid(), dto.sharePercentage(), BigDecimal::add);
        }
        shares.forEach((assessmentUuid, total) -> {
            if (total.compareTo(FULL_SHARE) != 0) {
                throw new IllegalArgumentException("Shares for one assessment must total 100% (currently " + total.stripTrailingZeros().toPlainString() + "%)");
            }
        });
    }

    private Set<UUID> rubricsOf(CourseAssessment assessment) {
        Set<UUID> rubrics = new HashSet<>();
        if (assessment.getRubricUuid() != null) {
            rubrics.add(assessment.getRubricUuid());
        }
        lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessment.getUuid()).stream()
                .map(CourseAssessmentLineItem::getRubricUuid)
                .filter(Objects::nonNull)
                .forEach(rubrics::add);
        return rubrics;
    }

    private UUID liveCourseUuid(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid)
                .map(course -> course.getParentCourseUuid() != null ? course.getParentCourseUuid() : course.getUuid())
                .orElse(courseUuid);
    }

    private ProgramSkillMappingDTO toDTO(ProgramSkillMapping mapping) {
        CourseAssessment assessment = courseAssessmentRepository.findByUuid(mapping.getCourseAssessmentUuid()).orElse(null);
        String criterion = mapping.getRubricCriteriaUuid() == null ? null
                : criteriaRepository.findByUuid(mapping.getRubricCriteriaUuid()).map(RubricCriteria::getComponentName).orElse(null);
        return new ProgramSkillMappingDTO(mapping.getUuid(), mapping.getCourseAssessmentUuid(), mapping.getRubricCriteriaUuid(),
                mapping.getSharePercentage(), assessment == null ? null : assessment.getCourseUuid(),
                assessment == null ? null : assessment.getTitle(), criterion);
    }
}
