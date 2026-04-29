package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.LessonPracticeActivityDTO;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.LessonPracticeActivity;
import apps.sarafrika.elimika.course.repository.LessonPracticeActivityRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityGrouping;
import apps.sarafrika.elimika.course.util.enums.PracticeActivityType;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonPracticeActivityServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonPracticeActivityRepository practiceActivityRepository;

    private LessonPracticeActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LessonPracticeActivityServiceImpl(lessonRepository, practiceActivityRepository);
    }

    @Test
    void createPracticeActivityAppliesDefaultsAndAppendsDisplayOrder() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        UUID activityUuid = UUID.randomUUID();

        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(courseUuid)));
        when(practiceActivityRepository.findMaxDisplayOrderByLessonUuid(lessonUuid)).thenReturn(3);
        when(practiceActivityRepository.save(any(LessonPracticeActivity.class))).thenAnswer(invocation -> {
            LessonPracticeActivity activity = invocation.getArgument(0);
            activity.setUuid(activityUuid);
            return activity;
        });

        LessonPracticeActivityDTO created = service.createPracticeActivity(
                courseUuid,
                lessonUuid,
                activityDTO(null, null, null, null)
        );

        ArgumentCaptor<LessonPracticeActivity> captor = ArgumentCaptor.forClass(LessonPracticeActivity.class);
        verify(practiceActivityRepository).save(captor.capture());

        LessonPracticeActivity saved = captor.getValue();
        assertThat(saved.getLessonUuid()).isEqualTo(lessonUuid);
        assertThat(saved.getActivityType()).isEqualTo(PracticeActivityType.EXERCISE);
        assertThat(saved.getGrouping()).isEqualTo(PracticeActivityGrouping.INDIVIDUAL);
        assertThat(saved.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(saved.getActive()).isFalse();
        assertThat(saved.getDisplayOrder()).isEqualTo(4);
        assertThat(created.uuid()).isEqualTo(activityUuid);
        assertThat(created.displayOrder()).isEqualTo(4);
    }

    @Test
    void createPracticeActivityRejectsMismatchedBodyLessonUuid() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();

        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(courseUuid)));

        LessonPracticeActivityDTO request = activityDTO(
                UUID.randomUUID(),
                PracticeActivityType.DISCUSSION,
                ContentStatus.DRAFT,
                false
        );

        assertThatThrownBy(() -> service.createPracticeActivity(courseUuid, lessonUuid, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lesson_uuid must match");

        verify(practiceActivityRepository, never()).save(any(LessonPracticeActivity.class));
    }

    @Test
    void createPracticeActivityRejectsActiveDraftActivity() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();

        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(courseUuid)));

        LessonPracticeActivityDTO request = activityDTO(
                null,
                PracticeActivityType.EXERCISE,
                ContentStatus.DRAFT,
                true
        );

        assertThatThrownBy(() -> service.createPracticeActivity(courseUuid, lessonUuid, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must be published");

        verify(practiceActivityRepository, never()).save(any(LessonPracticeActivity.class));
    }

    @Test
    void getPracticeActivitiesByLessonAppliesDefaultDisplayOrderSort() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        LessonPracticeActivity activity = practiceActivity(UUID.randomUUID(), lessonUuid, 2);

        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(courseUuid)));
        when(practiceActivityRepository.findByLessonUuid(eq(lessonUuid), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(activity),
                        invocation.getArgument(1),
                        1
                ));

        service.getPracticeActivitiesByLesson(courseUuid, lessonUuid, PageRequest.of(2, 10));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(practiceActivityRepository).findByLessonUuid(eq(lessonUuid), captor.capture());

        Pageable pageable = captor.getValue();
        Sort.Order order = pageable.getSort().getOrderFor("displayOrder");
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void reorderPracticeActivitiesRejectsPartialActivityList() {
        UUID courseUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();
        LessonPracticeActivity first = practiceActivity(UUID.randomUUID(), lessonUuid, 1);
        LessonPracticeActivity second = practiceActivity(UUID.randomUUID(), lessonUuid, 2);

        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson(courseUuid)));
        when(practiceActivityRepository.findByLessonUuidOrderByDisplayOrderAsc(lessonUuid))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.reorderPracticeActivities(
                courseUuid,
                lessonUuid,
                List.of(first.getUuid())
        ))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("include every activity");

        verify(practiceActivityRepository, never()).saveAll(any());
    }

    private Lesson lesson(UUID courseUuid) {
        Lesson lesson = new Lesson();
        lesson.setCourseUuid(courseUuid);
        return lesson;
    }

    private LessonPracticeActivityDTO activityDTO(UUID requestLessonUuid,
                                                  PracticeActivityType type,
                                                  ContentStatus status,
                                                  Boolean active) {
        return new LessonPracticeActivityDTO(
                null,
                requestLessonUuid,
                "Think-pair-share",
                "Discuss the prompt and share one observation.",
                type,
                null,
                15,
                new String[]{"Prompt sheet"},
                "One shared observation",
                null,
                status,
                active,
                null,
                null,
                null,
                null
        );
    }

    private LessonPracticeActivity practiceActivity(UUID activityUuid, UUID lessonUuid, int displayOrder) {
        LessonPracticeActivity activity = new LessonPracticeActivity();
        activity.setUuid(activityUuid);
        activity.setLessonUuid(lessonUuid);
        activity.setTitle("Activity " + displayOrder);
        activity.setInstructions("Run activity " + displayOrder);
        activity.setActivityType(PracticeActivityType.EXERCISE);
        activity.setGrouping(PracticeActivityGrouping.INDIVIDUAL);
        activity.setEstimatedMinutes(10);
        activity.setDisplayOrder(displayOrder);
        activity.setStatus(ContentStatus.DRAFT);
        activity.setActive(false);
        return activity;
    }
}
