package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.PrerequisiteTypeDTO;
import apps.sarafrika.elimika.course.repository.PrerequisiteTypeRepository;
import apps.sarafrika.elimika.course.service.PrerequisiteTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
class PrerequisiteTypeServiceImpl implements PrerequisiteTypeService {

    private static final String PREREQUISITE_TYPE_NOT_FOUND = "Prerequisite type not found.";
    private static final String PREREQUISITE_TYPE_FOUND_SUCCESS = "Prerequisite type has been retrieved successfully.";
    private static final String PREREQUISITE_TYPE_CREATED_SUCCESS = "Prerequisite type has been persisted successfully.";

    private final PrerequisiteTypeRepository prerequisiteTypeRepository;

    @Override
    public PrerequisiteTypeDTO createPrerequisiteType(PrerequisiteTypeDTO prerequisiteTypeDTO) {
        return null;
    }

    @Override
    public PrerequisiteTypeDTO getPrerequisiteTypeByUuid(UUID uuid) {
        return null;
    }

    @Override
    public Page<PrerequisiteTypeDTO> getAllPrerequisiteTypes(Pageable pageable) {
        return null;
    }

    @Override
    public PrerequisiteTypeDTO updatePrerequisiteType(UUID uuid, PrerequisiteTypeDTO prerequisiteTypeDTO) {
        return null;
    }

    @Override
    public void deletePrerequisiteType(UUID uuid) {

    }

    @Override
    public Page<PrerequisiteTypeDTO> searchPrerequisiteTypes(Map<String, String> searchParams, Pageable pageable) {
        return null;
    }
}
