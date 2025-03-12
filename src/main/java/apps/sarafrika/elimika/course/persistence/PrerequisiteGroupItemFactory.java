package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteGroupItemRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdatePrerequisiteGroupItemRequestDTO;

public class PrerequisiteGroupItemFactory {

    public static PrerequisiteGroupItem create(CreatePrerequisiteGroupItemRequestDTO createPrerequisiteGroupItemRequestDTO) {

        return PrerequisiteGroupItem.builder()
                .prerequisiteGroupId(createPrerequisiteGroupItemRequestDTO.prerequisiteGroupId())
                .prerequisiteId(createPrerequisiteGroupItemRequestDTO.prerequisiteId())
                .build();
    }

    public static void update(UpdatePrerequisiteGroupItemRequestDTO updatePrerequisiteGroupItemRequestDTO, PrerequisiteGroupItem prerequisiteGroupItem) {

        prerequisiteGroupItem.setPrerequisiteGroupId(updatePrerequisiteGroupItemRequestDTO.prerequisiteGroupId());
        prerequisiteGroupItem.setPrerequisiteId(updatePrerequisiteGroupItemRequestDTO.prerequisiteId());
    }
}
