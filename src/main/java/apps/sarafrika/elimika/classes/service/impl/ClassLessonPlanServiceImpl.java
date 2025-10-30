package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassLessonPlanDTO;
import apps.sarafrika.elimika.classes.factory.ClassLessonPlanFactory;
import apps.sarafrika.elimika.classes.model.ClassLessonPlan;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassLessonPlanRepository;
import apps.sarafrika.elimika.classes.service.ClassLessonPlanService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassLessonPlanServiceImpl implements ClassLessonPlanService {

    private static final String CLASS_NOT_FOUND_TEMPLATE = "Class definition with UUID %s not found";
    private static final String LESSON_PLAN_NOT_FOUND_TEMPLATE = "Lesson plan entry with UUID %s not found for class %s";
    private static final String LESSON_REQUIRED_MESSAGE = "Lesson UUID is required for lesson plan entries";

    private final ClassLessonPlanRepository classLessonPlanRepository;
    private final ClassDefinitionRepository classDefinitionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassLessonPlanDTO> getLessonPlan(UUID classDefinitionUuid) {
        ensureClassExists(classDefinitionUuid);
        return classLessonPlanRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .map(ClassLessonPlanFactory::toDTO)
                .toList();
    }

    @Override
    public List<ClassLessonPlanDTO> saveLessonPlan(UUID classDefinitionUuid, List<ClassLessonPlanDTO> requestedPlan) {
        ensureClassExists(classDefinitionUuid);

        List<ClassLessonPlan> existingPlans = classLessonPlanRepository.findByClassDefinitionUuid(classDefinitionUuid);
        Map<UUID, ClassLessonPlan> remainingPlans = new HashMap<>();
        for (ClassLessonPlan plan : existingPlans) {
            if (plan.getUuid() != null) {
                remainingPlans.put(plan.getUuid(), plan);
            }
        }

        List<ClassLessonPlan> entitiesToPersist = new ArrayList<>();
        if (requestedPlan != null) {
            for (ClassLessonPlanDTO dto : requestedPlan) {
                if (dto.lessonUuid() == null) {
                    throw new ValidationException(LESSON_REQUIRED_MESSAGE);
                }
                if (dto.uuid() != null) {
                    ClassLessonPlan existing = remainingPlans.remove(dto.uuid());
                    if (existing == null) {
                        throw new ResourceNotFoundException(
                                String.format(LESSON_PLAN_NOT_FOUND_TEMPLATE, dto.uuid(), classDefinitionUuid));
                    }
                    ClassLessonPlanFactory.updateEntityFromDTO(existing, dto);
                    existing.setClassDefinitionUuid(classDefinitionUuid);
                    existing.setLessonUuid(dto.lessonUuid());
                    entitiesToPersist.add(existing);
                } else {
                    ClassLessonPlan created = ClassLessonPlanFactory.toEntity(dto);
                    created.setClassDefinitionUuid(classDefinitionUuid);
                    created.setLessonUuid(dto.lessonUuid());
                    entitiesToPersist.add(created);
                }
            }
        }

        if (!remainingPlans.isEmpty()) {
            log.debug("Removing {} lesson plan entries not present in update for class {}", remainingPlans.size(), classDefinitionUuid);
            classLessonPlanRepository.deleteAll(remainingPlans.values());
        }

        if (!entitiesToPersist.isEmpty()) {
            classLessonPlanRepository.saveAll(entitiesToPersist);
        }

        classLessonPlanRepository.flush();

        return classLessonPlanRepository.findByClassDefinitionUuid(classDefinitionUuid)
                .stream()
                .map(ClassLessonPlanFactory::toDTO)
                .toList();
    }

    private void ensureClassExists(UUID classDefinitionUuid) {
        classDefinitionRepository.findByUuid(classDefinitionUuid)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(CLASS_NOT_FOUND_TEMPLATE, classDefinitionUuid)));
    }
}
