package apps.sarafrika.elimika.course.application;

import apps.sarafrika.elimika.course.api.ClassService;
import apps.sarafrika.elimika.course.api.dto.request.CreateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.request.UpdateClassRequestDTO;
import apps.sarafrika.elimika.course.api.dto.response.ClassResponseDTO;
import apps.sarafrika.elimika.course.application.exceptions.ClassNotFoundException;
import apps.sarafrika.elimika.course.application.exceptions.CourseNotFoundException;
import apps.sarafrika.elimika.course.application.exceptions.InstructorAvailabilityNotFoundException;
import apps.sarafrika.elimika.course.application.exceptions.InstructorNotFoundException;
import apps.sarafrika.elimika.course.domain.Class;
import apps.sarafrika.elimika.course.domain.*;
import apps.sarafrika.elimika.shared.dto.ResponseDTO;
import apps.sarafrika.elimika.shared.dto.ResponsePageableDTO;
import lombok.RequiredArgsConstructor;
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
public class ClassServiceImpl implements ClassService {

    private static final String ERROR_CLASS_NOT_FOUND = "Class not found";
    private static final String ERROR_COURSE_NOT_FOUND = "Course not found.";
    private static final String ERROR_INSTRUCTOR_NOT_FOUND = "Instructor not found.";
    private static final String ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND = "Instructor availability not found";
    private static final String CLASS_FOUND_SUCCESS = "Class retrieved successfully";
    private static final String CLASS_CREATED_SUCCESS = "Class created successfully";
    private static final String CLASS_UPDATED_SUCCESS = "Class updated successfully";
    private static final String CLASSES_FOUND_SUCCESS = "Classes retrieved successfully";

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorAvailabilityRepository instructorAvailabilityRepository;


    @Override
    public ResponseDTO<ClassResponseDTO> findById(Long id) {

        final Class classEntity = findClassById(id);

        ClassResponseDTO classResponseDTO = ClassResponseDTO.from(classEntity);

        return new ResponseDTO<>(classResponseDTO, HttpStatus.OK.value(), CLASS_FOUND_SUCCESS, null, LocalDateTime.now());
    }


    @Override
    @Transactional(readOnly = true)
    public ResponsePageableDTO<ClassResponseDTO> findAll(Pageable pageable) {

        Page<ClassResponseDTO> classesPage = classRepository.findAll(pageable)
                .stream()
                .map(ClassResponseDTO::from)
                .collect(Collectors.collectingAndThen(Collectors.toList(), PageImpl::new));

        return new ResponsePageableDTO<>(classesPage.getContent(), classesPage.getNumber(), classesPage.getSize(),
                classesPage.getTotalPages(), classesPage.getTotalElements(), HttpStatus.OK.value(), CLASSES_FOUND_SUCCESS);
    }


    @Override
    @Transactional
    public ResponseDTO<Void> create(CreateClassRequestDTO createClassRequestDTO) {

        final Instructor instructor = findInstructorById(createClassRequestDTO.instructorId());

        final Course course = findCourseById(createClassRequestDTO.courseId());

        final InstructorAvailability availabilitySlot = findInstructorAvailabilityByIdAndInstructor(createClassRequestDTO.availabilitySlotId(), instructor);

        Class classEntity = ClassFactory.create(createClassRequestDTO, instructor, course, availabilitySlot);

        classRepository.save(classEntity);

        return new ResponseDTO<>(null, HttpStatus.CREATED.value(), CLASS_CREATED_SUCCESS, null, LocalDateTime.now());
    }


    @Override
    @Transactional
    public ResponseDTO<Void> update(UpdateClassRequestDTO updateClassRequestDTO, Long id) {

        final Class classEntity = findClassById(id);

        final Instructor instructor = findInstructorById(updateClassRequestDTO.instructorId());

        final InstructorAvailability availabilitySlot = findInstructorAvailabilityByIdAndInstructor(updateClassRequestDTO.availabilitySlotId(), instructor);

        ClassFactory.update(updateClassRequestDTO, classEntity);

        classEntity.setInstructor(instructor);

        classEntity.setAvailabilitySlot(availabilitySlot);

        classRepository.save(classEntity);

        return new ResponseDTO<>(null, HttpStatus.OK.value(), CLASS_UPDATED_SUCCESS, null, LocalDateTime.now());
    }


    @Override
    @Transactional
    public void delete(final Long id) {

        final Class classEntity = findClassById(id);

        classRepository.delete(classEntity);
    }


    private Class findClassById(final Long id) {

        return classRepository.findById(id).orElseThrow(() -> new ClassNotFoundException(ERROR_CLASS_NOT_FOUND));
    }


    private Instructor findInstructorById(Long id) {

        return instructorRepository.findById(id).orElseThrow(() -> new InstructorNotFoundException(ERROR_INSTRUCTOR_NOT_FOUND));
    }


    private Course findCourseById(Long id) {

        return courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(ERROR_COURSE_NOT_FOUND));
    }

    private InstructorAvailability findInstructorAvailabilityByIdAndInstructor(Long id, Instructor instructor) {

        return instructorAvailabilityRepository.findByIdAndInstructor(id, instructor).orElseThrow(() -> new InstructorAvailabilityNotFoundException(ERROR_INSTRUCTOR_AVAILABILITY_NOT_FOUND));
    }
}

