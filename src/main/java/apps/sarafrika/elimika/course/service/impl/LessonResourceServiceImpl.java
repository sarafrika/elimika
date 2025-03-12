package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.config.exception.LessonResourceNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateLessonResourceRequestDTO;
import apps.sarafrika.elimika.course.dto.request.LessonResouceRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateLessonResourceRequestDTO;
import apps.sarafrika.elimika.course.dto.response.LessonResourceResponseDTO;
import apps.sarafrika.elimika.course.persistence.LessonResource;
import apps.sarafrika.elimika.course.persistence.LessonResourceFactory;
import apps.sarafrika.elimika.course.persistence.LessonResourceRepository;
import apps.sarafrika.elimika.course.persistence.LessonResourceSpecification;
import apps.sarafrika.elimika.course.service.LessonResourceService;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
class LessonResourceServiceImpl implements LessonResourceService {

    private static final String ERROR_LESSON_RESOURCE_NOT_FOUND = "Lesson resource not found.";
    private static final String LESSON_RESOURCE_FOUND_SUCCESS = "Lesson resource retrieved successfully.";
    private static final String LESSON_RESOURCE_CREATED_SUCCESS = "Lesson resource persisted successfully.";
    private static final String LESSON_RESOURCE_UPDATED_SUCCESS = "Lesson resource updated successfully.";

    private final LessonResourceRepository lessonResourceRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<List<LessonResourceResponseDTO>> findLessonResources(LessonResouceRequestDTO lessonResouceRequestDTO) {

        final Specification<LessonResource> specification = new LessonResourceSpecification(lessonResouceRequestDTO);

        final List<LessonResource> lessonResources = lessonResourceRepository.findAll(specification);

        List<LessonResourceResponseDTO> lessonResourceResponseDTOs = lessonResources.stream()
                .map(LessonResourceResponseDTO::from)
                .toList();

        return new ResponseDTO<>(lessonResourceResponseDTOs, HttpStatus.OK.value(), LESSON_RESOURCE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDTO<LessonResourceResponseDTO> findLessonResource(Long lessonResourceId) {

        final LessonResource lessonResource = findLessonResourceById(lessonResourceId);

        LessonResourceResponseDTO lessonResourceResponseDTO = LessonResourceResponseDTO.from(lessonResource);

        return new ResponseDTO<>(lessonResourceResponseDTO, HttpStatus.OK.value(), LESSON_RESOURCE_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private LessonResource findLessonResourceById(Long lessonResourceId) {

        return lessonResourceRepository.findById(lessonResourceId).orElseThrow(() -> new LessonResourceNotFoundException(ERROR_LESSON_RESOURCE_NOT_FOUND));
    }

    @Transactional
    @Override
    public ResponseDTO<LessonResourceResponseDTO> createLessonResource(Long lessonId, CreateLessonResourceRequestDTO createLessonResourceRequestDTO) {

        LessonResource lessonResource = LessonResourceFactory.create(createLessonResourceRequestDTO);
        lessonResource.setLessonId(lessonId);

        lessonResourceRepository.save(lessonResource);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), LESSON_RESOURCE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<List<LessonResourceResponseDTO>> createLessonResources(Long lessonId, List<CreateLessonResourceRequestDTO> createLessonResourceRequestDTOs) {

        List<LessonResource> lessonResources = createLessonResourceRequestDTOs.stream()
                .map(LessonResourceFactory::create)
                .map(lessonResource -> {
                    lessonResource.setLessonId(lessonId);
                    return lessonResource;
                })
                .toList();

        lessonResourceRepository.saveAll(lessonResources);

        List<LessonResourceResponseDTO> lessonResourceResponseDTOs = lessonResources.stream()
                .map(LessonResourceResponseDTO::from)
                .toList();

        return new ResponseDTO<>(lessonResourceResponseDTOs, HttpStatus.CREATED.value(), LESSON_RESOURCE_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Transactional
    @Override
    public ResponseDTO<LessonResourceResponseDTO> updateLessonResource(Long lessonResourceId, UpdateLessonResourceRequestDTO updateLessonResourceRequestDTO) {

        LessonResource lessonResource = findLessonResourceById(lessonResourceId);

        LessonResourceFactory.update(lessonResource, updateLessonResourceRequestDTO);

        lessonResourceRepository.save(lessonResource);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), LESSON_RESOURCE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<List<LessonResourceResponseDTO>> updateLessonResources(Long lessonId, List<UpdateLessonResourceRequestDTO> updateLessonResourceRequestDTOs) {

        List<LessonResource> lessonResources = updateLessonResourceRequestDTOs.stream()
                .map(updateLessonResourceRequestDTO -> {
                    LessonResource foundResource = findLessonResourceByIdAndLessonId(lessonId, updateLessonResourceRequestDTO.id());
                    LessonResourceFactory.update(foundResource, updateLessonResourceRequestDTO);
                    return foundResource;
                })
                .toList();

        lessonResourceRepository.saveAll(lessonResources);

        List<LessonResourceResponseDTO> lessonResourceResponseDTOs = lessonResources.stream()
                .map(LessonResourceResponseDTO::from)
                .toList();

        return new ResponseDTO<>(lessonResourceResponseDTOs, HttpStatus.OK.value(), LESSON_RESOURCE_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    private LessonResource findLessonResourceByIdAndLessonId(Long lessonId, Long lessonResourceId) {

        return lessonResourceRepository.findByIdAndLessonId(lessonResourceId, lessonId).orElseThrow(() -> new LessonResourceNotFoundException(ERROR_LESSON_RESOURCE_NOT_FOUND));
    }

    @Transactional
    @Override
    public void deleteLessonResource(Long lessonResourceId) {

        final LessonResource lessonResource = findLessonResourceById(lessonResourceId);

        lessonResourceRepository.delete(lessonResource);
    }
}
