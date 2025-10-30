package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassAssignmentScheduleDTO;
import apps.sarafrika.elimika.classes.dto.ClassQuizScheduleDTO;

import java.util.List;
import java.util.UUID;

public interface ClassAssessmentScheduleService {

    // Assignment schedules
    List<ClassAssignmentScheduleDTO> getAssignmentSchedules(UUID classDefinitionUuid);

    ClassAssignmentScheduleDTO createAssignmentSchedule(UUID classDefinitionUuid, ClassAssignmentScheduleDTO request);

    ClassAssignmentScheduleDTO updateAssignmentSchedule(UUID classDefinitionUuid, UUID scheduleUuid, ClassAssignmentScheduleDTO request);

    void deleteAssignmentSchedule(UUID classDefinitionUuid, UUID scheduleUuid);

    // Quiz schedules
    List<ClassQuizScheduleDTO> getQuizSchedules(UUID classDefinitionUuid);

    ClassQuizScheduleDTO createQuizSchedule(UUID classDefinitionUuid, ClassQuizScheduleDTO request);

    ClassQuizScheduleDTO updateQuizSchedule(UUID classDefinitionUuid, UUID scheduleUuid, ClassQuizScheduleDTO request);

    void deleteQuizSchedule(UUID classDefinitionUuid, UUID scheduleUuid);
}
