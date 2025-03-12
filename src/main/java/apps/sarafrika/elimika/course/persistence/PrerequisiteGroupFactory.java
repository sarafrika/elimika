package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteGroupRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdatePrerequisiteGroupRequestDTO;

public class PrerequisiteGroupFactory {

    public static PrerequisiteGroup create(CreatePrerequisiteGroupRequestDTO createPrerequisiteGroupRequestDTO) {

        return PrerequisiteGroup.builder()
                .courseId(createPrerequisiteGroupRequestDTO.courseId())
                .groupType(createPrerequisiteGroupRequestDTO.groupType())
                .build();
    }

    public static void update(UpdatePrerequisiteGroupRequestDTO updatePrerequisiteGroupRequestDTO, PrerequisiteGroup prerequisiteGroup) {

        prerequisiteGroup.setCourseId(updatePrerequisiteGroupRequestDTO.courseId());
        prerequisiteGroup.setGroupType(updatePrerequisiteGroupRequestDTO.groupType());
    }
}
