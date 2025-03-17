package apps.sarafrika.elimika.course.dto.request;

import apps.sarafrika.elimika.course.model.PrerequisiteGroup;

import java.util.List;

public record CreatePrerequisiteGroupRequestDTO(
        Long courseId,
        PrerequisiteGroup.GroupType groupType,
        List<Long> prerequisiteIds
) {
}
