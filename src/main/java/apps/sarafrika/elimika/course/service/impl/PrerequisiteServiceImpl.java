package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.PrerequisiteGroupNotFoundException;
import apps.sarafrika.elimika.course.config.exception.PrerequisiteNotFoundException;
import apps.sarafrika.elimika.course.dto.request.*;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteGroupResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteResponseDTO;
import apps.sarafrika.elimika.course.dto.response.PrerequisiteTypeResponseDTO;
import apps.sarafrika.elimika.course.persistence.*;
import apps.sarafrika.elimika.course.service.CourseService;
import apps.sarafrika.elimika.course.service.PrerequisiteService;
import apps.sarafrika.elimika.course.service.PrerequisiteTypeService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public ResponsePageableDTO<PrerequisiteResponseDTO> findAllPrerequisites(PrerequisiteRequestDTO prerequisiteRequestDTO, Pageable pageable) {

        final Specification<Prerequisite> specification = new PrerequisiteSpecification(prerequisiteRequestDTO);

        final Page<Prerequisite> prerequisites = prerequisiteRepository.findAll(specification, pageable);

        List<PrerequisiteResponseDTO> prerequisiteResponseDTOs = prerequisites.stream()
                .map(this::toResponseDTO)
                .toList();

        return new ResponsePageableDTO<>(prerequisiteResponseDTOs, prerequisites.getNumber(), prerequisites.getSize(),
                prerequisites.getTotalPages(), prerequisites.getTotalElements(), HttpStatus.OK.value(), PREREQUISITES_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<PrerequisiteResponseDTO> findPrerequisite(Long prerequisiteId) {

        Prerequisite prerequisite = findPrerequisiteById(prerequisiteId);

        PrerequisiteResponseDTO prerequisiteResponseDTO = toResponseDTO(prerequisite);

        return new ResponseDTO<>(prerequisiteResponseDTO, HttpStatus.OK.value(), PREREQUISITE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private PrerequisiteResponseDTO toResponseDTO(final Prerequisite prerequisite) {

        ResponseDTO<CourseResponseDTO> course = courseService.findCourse(prerequisite.getCourseId());

        ResponseDTO<CourseResponseDTO> requiredCourse = courseService.findCourse(prerequisite.getRequiredForCourseId());

        ResponseDTO<PrerequisiteTypeResponseDTO> prerequisiteType = prerequisiteTypeService.findPrerequisiteType(prerequisite.getPrerequisiteTypeId());

        return new PrerequisiteResponseDTO(prerequisite.getId(), prerequisiteType.data(), course.data(), requiredCourse.data(), prerequisite.getMinimumScore());
    }

    private Prerequisite findPrerequisiteById(Long prerequisiteId) {

        return prerequisiteRepository.findById(prerequisiteId).orElseThrow(() -> new PrerequisiteNotFoundException(PREREQUISITE_NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createPrerequisite(CreatePrerequisiteRequestDTO createPrerequisiteRequestDTO) {

        Prerequisite prerequisite = new Prerequisite();

        mapToPrerequisite(createPrerequisiteRequestDTO.courseId(), createPrerequisiteRequestDTO.requiredForCourseId(), createPrerequisiteRequestDTO.prerequisiteTypeId(),
                createPrerequisiteRequestDTO.minimumScore(), prerequisite);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), PREREQUISITE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updatePrerequisite(UpdatePrerequisiteRequestDTO updatePrerequisiteRequestDTO, Long prerequisiteId) {

        final Prerequisite prerequisite = findPrerequisiteById(prerequisiteId);

        mapToPrerequisite(updatePrerequisiteRequestDTO.courseId(), updatePrerequisiteRequestDTO.requiredForCourseId(), updatePrerequisiteRequestDTO.prerequisiteTypeId(),
                updatePrerequisiteRequestDTO.minimumScore(), prerequisite);

        prerequisiteRepository.save(prerequisite);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), PREREQUISITE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    private void mapToPrerequisite(Long courseId, Long requiredForCourseId, Long prerequisiteTypeId, double minimumScore, Prerequisite prerequisite) {
        ResponseDTO<PrerequisiteTypeResponseDTO> prerequisiteType = prerequisiteTypeService.findPrerequisiteType(prerequisiteTypeId);
        prerequisite.setPrerequisiteTypeId(prerequisiteType.data().id());

        ResponseDTO<CourseResponseDTO> course = courseService.findCourse(courseId);
        prerequisite.setCourseId(course.data().id());

        ResponseDTO<CourseResponseDTO> requiredForCourse = courseService.findCourse(requiredForCourseId);
        prerequisite.setRequiredForCourseId(requiredForCourse.data().id());

        prerequisite.setMinimumScore(minimumScore);
    }

    @Transactional
    @Override
    public void deletePrerequisite(Long prerequisiteId) {

        Prerequisite prerequisite = findPrerequisiteById(prerequisiteId);

        prerequisiteRepository.delete(prerequisite);
    }

    @Transactional
    @Override
    public ResponseDTO<Void> createPrerequisiteGroup(CreatePrerequisiteGroupRequestDTO createPrerequisiteGroupRequestDTO) {

        PrerequisiteGroup prerequisiteGroup = PrerequisiteGroupFactory.create(createPrerequisiteGroupRequestDTO);

        ResponseDTO<CourseResponseDTO> course = courseService.findCourse(createPrerequisiteGroupRequestDTO.courseId());
        prerequisiteGroup.setCourseId(course.data().id());

        PrerequisiteGroup savedPrerequisiteGroup = prerequisiteGroupRepository.save(prerequisiteGroup);

        List<PrerequisiteGroupItem> prerequisiteGroupItems = createPrerequisiteGroupRequestDTO.prerequisiteIds().stream()
                .map(prerequisiteId -> PrerequisiteGroupItemFactory.create(new CreatePrerequisiteGroupItemRequestDTO(savedPrerequisiteGroup.getId(), prerequisiteId)))
                .toList();

        prerequisiteGroupItemRepository.saveAll(prerequisiteGroupItems);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), PREREQUISITE_GROUP_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<Void> updatePrerequisiteGroup(UpdatePrerequisiteGroupRequestDTO updatePrerequisiteGroupRequestDTO, Long prerequisiteGroupId) {

        PrerequisiteGroup prerequisiteGroup = findPrerequisiteGroupById(prerequisiteGroupId);

        PrerequisiteGroupFactory.update(updatePrerequisiteGroupRequestDTO, prerequisiteGroup);

        prerequisiteGroupRepository.save(prerequisiteGroup);

        prerequisiteGroupItemRepository.deleteAllByPrerequisiteGroupId(prerequisiteGroupId);

        List<PrerequisiteGroupItem> prerequisiteGroupItems = updatePrerequisiteGroupRequestDTO.prerequisiteIds().stream()
                .map(prerequisiteId -> PrerequisiteGroupItemFactory.create(new CreatePrerequisiteGroupItemRequestDTO(prerequisiteGroup.getId(), prerequisiteId)))
                .toList();

        prerequisiteGroupItemRepository.saveAll(prerequisiteGroupItems);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), PREREQUISITE_GROUP_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    private PrerequisiteGroup findPrerequisiteGroupById(Long prerequisiteGroupId) {

        return prerequisiteGroupRepository.findById(prerequisiteGroupId).orElseThrow(() -> new PrerequisiteGroupNotFoundException(PREREQUISITE_GROUP_NOT_FOUND));
    }

    @Override
    public ResponseDTO<PrerequisiteGroupResponseDTO> findPrerequisiteGroup(Long prerequisiteGroupId) {

        PrerequisiteGroup prerequisiteGroup = findPrerequisiteGroupById(prerequisiteGroupId);

        return null;
    }

    @Transactional
    @Override
    public void deletePrerequisiteGroup(Long prerequisiteGroupId) {

        PrerequisiteGroup prerequisiteGroup = findPrerequisiteGroupById(prerequisiteGroupId);

        prerequisiteGroupRepository.delete(prerequisiteGroup);
    }

}
