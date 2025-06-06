package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.PrerequisiteTypeNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreatePrerequisiteTypeRequestDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteTypeResponseDTO;
import apps.sarafrika.elimika.course.persistence.PrerequisiteType;
import apps.sarafrika.elimika.course.persistence.PrerequisiteTypeRepository;
import apps.sarafrika.elimika.course.service.PrerequisiteTypeService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
class PrerequisiteTypeServiceImpl implements PrerequisiteTypeService {

    private static final String PREREQUISITE_TYPE_NOT_FOUND = "Prerequisite type not found.";
    private static final String PREREQUISITE_TYPE_FOUND_SUCCESS = "Prerequisite type has been retrieved successfully.";
    private static final String PREREQUISITE_TYPE_CREATED_SUCCESS = "Prerequisite type has been persisted successfully.";

    private final PrerequisiteTypeRepository prerequisiteTypeRepository;

    @Override
    public ResponsePageableDTO<PrerequisiteTypeResponseDTO> findAllPrerequisiteTypes(Pageable pageable) {

        Page<PrerequisiteType> prerequisiteTypes = prerequisiteTypeRepository.findAll(pageable);

        List<PrerequisiteTypeResponseDTO> prerequisiteTypeResponseDTOS = prerequisiteTypes.stream()
                .map(PrerequisiteTypeResponseDTO::from)
                .toList();

        return new ResponsePageableDTO<>(prerequisiteTypeResponseDTOS, prerequisiteTypes.getNumber(), prerequisiteTypes.getSize(), prerequisiteTypes.getTotalPages()
                , prerequisiteTypes.getTotalElements(), HttpStatus.OK.value(), PREREQUISITE_TYPE_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<PrerequisiteTypeResponseDTO> findPrerequisiteType(Long id) {

        PrerequisiteType prerequisiteType = findPrerequisiteTypeById(id);

        PrerequisiteTypeResponseDTO prerequisiteTypeResponseDTO = PrerequisiteTypeResponseDTO.from(prerequisiteType);

        return new ResponseDTO<>(prerequisiteTypeResponseDTO, HttpStatus.OK.value(), PREREQUISITE_TYPE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> createPrerequisiteType(CreatePrerequisiteTypeRequestDTO createPrerequisiteTypeRequestDTO) {

        PrerequisiteType prerequisiteType = PrerequisiteType.builder()
                .name(createPrerequisiteTypeRequestDTO.name())
                .build();

        prerequisiteTypeRepository.save(prerequisiteType);
        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), PREREQUISITE_TYPE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    private PrerequisiteType findPrerequisiteTypeById(Long id) {

        return prerequisiteTypeRepository.findById(id).orElseThrow(() -> new PrerequisiteTypeNotFoundException(PREREQUISITE_TYPE_NOT_FOUND));
    }
}
