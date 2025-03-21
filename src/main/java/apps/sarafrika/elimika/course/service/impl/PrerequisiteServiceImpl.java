package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.PrerequisiteDTO;
import apps.sarafrika.elimika.course.repository.PrerequisiteGroupItemRepository;
import apps.sarafrika.elimika.course.repository.PrerequisiteGroupRepository;
import apps.sarafrika.elimika.course.repository.PrerequisiteRepository;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.PrerequisiteService;
import apps.sarafrika.elimika.course.service.PrerequisiteTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PrerequisiteServiceImpl implements PrerequisiteService {

    private static final String PREREQUISITE_CREATED_SUCCESS = "Prerequisite has been persisted successfully.";
    private static final String PREREQUISITE_NOT_FOUND = "Prerequisite not found.";
    private static final String PREREQUISITE_GROUP_NOT_FOUND = "Prerequisite not found.";
    private static final String PREREQUISITE_FOUND_SUCCESS = "Prerequisite has been retrieved successfully.";
    private static final String PREREQUISITE_UPDATED_SUCCESS = "Prerequisite has been updated successfully.";
    private static final String PREREQUISITE_GROUP_CREATED_SUCCESS = "Prerequisite group has been persisted successfully.";
    private static final String PREREQUISITE_GROUP_UPDATED_SUCCESS = "Prerequisite group has been updated successfully.";
    private static final String PREREQUISITES_FOUND_SUCCESS = "Prerequisites retrieved successfully.";

    private final CourseService courseService;
    private final PrerequisiteRepository prerequisiteRepository;
    private final PrerequisiteTypeService prerequisiteTypeService;
    private final PrerequisiteGroupRepository prerequisiteGroupRepository;
    private final PrerequisiteGroupItemRepository prerequisiteGroupItemRepository;

    @Override
    public PrerequisiteDTO createPrerequisite(PrerequisiteDTO prerequisiteDTO) {
        return null;
    }

    @Override
    public PrerequisiteDTO getPrerequisiteByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<PrerequisiteDTO> getAllPrerequisites(Pageable pageable) {
        return null;
    }

    @Override
    public PrerequisiteDTO updatePrerequisite(UUID uuid, PrerequisiteDTO prerequisiteDTO) {
        return null;
    }

    @Override
    public void deletePrerequisite(UUID uuid) {

    }

    @Override
    public Page<PrerequisiteDTO> searchPrerequisites(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
