package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassLessonPlanDTO;

import java.util.List;
import java.util.UUID;

public interface ClassLessonPlanService {

    List<ClassLessonPlanDTO> getLessonPlan(UUID classDefinitionUuid);

    List<ClassLessonPlanDTO> saveLessonPlan(UUID classDefinitionUuid, List<ClassLessonPlanDTO> requestedPlan);
}
