package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.availability.spi.AvailabilityService;
import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.RecurrencePatternRepository;
import apps.sarafrika.elimika.classes.util.enums.LocationType;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassDefinitionServiceImplTest {

    @Mock
    private ClassDefinitionRepository classDefinitionRepository;

    @Mock
    private RecurrencePatternRepository recurrencePatternRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ClassDefinitionServiceImpl classDefinitionService;

    @Test
    void createClassDefinition_whenTrainingFeeBelowCourseMinimum_throwsIllegalArgumentException() {
        UUID courseUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();

        Course course = new Course();
        course.setMinimumTrainingFee(new BigDecimal("200.00"));

        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
        when(availabilityService.getAvailabilityForInstructor(instructorUuid)).thenReturn(List.of());

        ClassDefinitionDTO dto = new ClassDefinitionDTO(
                null,
                "Advanced Robotics Workshop",
                "Deep dive into robotics",
                instructorUuid,
                UUID.randomUUID(),
                courseUuid,
                new BigDecimal("150.00"),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                LocationType.IN_PERSON,
                20,
                true,
                null,
                true,
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> classDefinitionService.createClassDefinition(dto));

        verify(classDefinitionRepository, never()).save(any(ClassDefinition.class));
    }
}
