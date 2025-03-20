package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.PrerequisiteGroupItemDTO;
import apps.sarafrika.elimika.course.dto.request.UpdatePrerequisiteGroupItemRequestDTO;
import apps.sarafrika.elimika.course.model.PrerequisiteGroupItem;

public class PrerequisiteGroupItemFactory {

    public static PrerequisiteGroupItem create(PrerequisiteGroupItemDTO createPrerequisiteGroupItemRequestDTO) {

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
