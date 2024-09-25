package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.config.exception.ClassNotFoundException;
import apps.sarafrika.elimika.course.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.course.dto.response.CourseResponseDTO;
import apps.sarafrika.elimika.course.event.CreateClassEvent;
import apps.sarafrika.elimika.course.event.UpdateClassEvent;
import apps.sarafrika.elimika.course.persistence.Class;
import apps.sarafrika.elimika.course.persistence.ClassFactory;
import apps.sarafrika.elimika.course.persistence.ClassRepository;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ClassServiceImpl implements ClassService {

    private static final String ERROR_CLASS_NOT_FOUND = "Class not found";
    private static final String CLASS_FOUND_SUCCESS = "Class retrieved successfully";
    private static final String CLASS_CREATED_SUCCESS = "Class created successfully";
    private static final String CLASSES_FOUND_SUCCESS = "Classes retrieved successfully";
    private static final String CLASS_UPDATED_SUCCESS = "Class updated successfully";

    private final CourseService courseService;
    private final ClassRepository classRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Override
    public ResponsePageableDTO<ClassResponseDTO> findAllClasses(Pageable pageable) {

        Page<ClassResponseDTO> classesPage = classRepository.findAll(pageable)
                .stream()
                .map(ClassResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(classesPage.getContent(), classesPage.getNumber(), classesPage.getSize(),
                classesPage.getTotalPages(), classesPage.getTotalElements(), HttpStatus.OK.value(), CLASSES_FOUND_SUCCESS);
    }

    @Override
    public ResponseDTO<ClassResponseDTO> findClass(Long id) {

        final Class classEntity = findClassById(id);

        ClassResponseDTO classResponseDTO = ClassResponseDTO.from(classEntity);

        return new ResponseDTO<>(classResponseDTO, HttpStatus.OK.value(), CLASS_FOUND_SUCCESS, null, LocalDateTime.now());
    }

    private Class findClassById(final Long id) {

        return classRepository.findById(id).orElseThrow(() -> new ClassNotFoundException(ERROR_CLASS_NOT_FOUND));
    }

    @Override
    public ResponseDTO<Void> createClass(CreateClassRequestDTO createClassRequestDTO) {
        final ResponseDTO<CourseResponseDTO> course = courseService.findCourse(createClassRequestDTO.courseId());

        Class classEntity = ClassFactory.create(createClassRequestDTO);

        classEntity.setCourseId(course.data().id());

        eventPublisher.publishEvent(new CreateClassEvent(classEntity, createClassRequestDTO));

        classRepository.save(classEntity);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), CLASS_CREATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public ResponseDTO<Void> updateClass(UpdateClassRequestDTO updateClassRequestDTO, Long id) {
        final Class classEntity = findClassById(id);

        ClassFactory.update(updateClassRequestDTO, classEntity);

        eventPublisher.publishEvent(new UpdateClassEvent(classEntity, updateClassRequestDTO));

        classRepository.save(classEntity);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), CLASS_UPDATED_SUCCESS, null, LocalDateTime.now());
    }

    @Override
    public void deleteClass(Long id) {

        final Class classEntity = findClassById(id);

        classRepository.delete(classEntity);
    }
}
