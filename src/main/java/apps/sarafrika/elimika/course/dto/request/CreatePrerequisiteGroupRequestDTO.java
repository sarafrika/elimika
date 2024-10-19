package apps.sarafrika.elimika.course.dto.request;

import apps.sarafrika.elimika.course.persistence.PrerequisiteGroup;

import java.util.List;

public record CreatePrerequisiteGroupRequestDTO(
        Long courseId,
        PrerequisiteGroup.GroupType groupType,
        List<Long> prerequisiteIds
) {
}
