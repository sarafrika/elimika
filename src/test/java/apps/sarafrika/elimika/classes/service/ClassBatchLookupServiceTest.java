package apps.sarafrika.elimika.classes.service;

import apps.sarafrika.elimika.classes.dto.ClassBatchSummaryDTO;
import apps.sarafrika.elimika.classes.internal.ClassListingVisibility;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.instructor.spi.InstructorDirectoryEntry;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.security.ActingDomainCap;
import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassBatchLookupServiceTest {

    private static final ClassListingVisibility.Scope PUBLIC_ONLY =
            new ClassListingVisibility.Scope(false, Set.of(), null, Set.of());

    @Mock private ClassDefinitionRepository classDefinitionRepository;
    @Mock private ClassListingVisibility classListingVisibility;
    @Mock private CourseInfoService courseInfoService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private ObjectProvider<TimetableService> timetableServiceProvider;
    @Mock private TimetableService timetableService;
    @Mock private DomainSecurityService domainSecurityService;

    private ClassBatchLookupService service;

    @BeforeEach
    void setUp() {
        // Built for real: outside a request the cap permits every domain.
        ActingDomainCap cap = new ActingDomainCap(new ActingDomainResolver(new RequestScopedCache()));
        service = new ClassBatchLookupService(classDefinitionRepository, classListingVisibility, courseInfoService,
                instructorLookupService, timetableServiceProvider, domainSecurityService, cap);
        when(timetableServiceProvider.getIfAvailable()).thenReturn(timetableService);
        when(classListingVisibility.forCurrentCaller()).thenReturn(PUBLIC_ONLY);
    }

    @Test
    void joinsTitlesAndInstructorsInRequestOrderWithOneReadPerSource() {
        UUID courseUuid = UUID.randomUUID();
        UUID programUuid = UUID.randomUUID();
        UUID instructorUuid = UUID.randomUUID();
        ClassDefinition first = publicClass(courseUuid, null, instructorUuid, UUID.randomUUID());
        ClassDefinition second = publicClass(null, programUuid, null, UUID.randomUUID());
        UUID unknown = UUID.randomUUID();
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(List.of(first, second));
        when(courseInfoService.getCourseNames(anyCollection())).thenReturn(Map.of(courseUuid, "Piano Basics"));
        when(courseInfoService.getTrainingProgramTitles(anyCollection())).thenReturn(Map.of(programUuid, "Music Track"));
        when(instructorLookupService.findInstructorDirectoryEntries(anyCollection())).thenReturn(Map.of(
                instructorUuid, new InstructorDirectoryEntry(instructorUuid, "Jane Doe", "Nairobi", true)));

        List<ClassBatchSummaryDTO> result = service.findByUuids(List.of(second.getUuid(), unknown, first.getUuid()));

        assertThat(result).extracting(ClassBatchSummaryDTO::uuid).containsExactly(second.getUuid(), first.getUuid());
        assertThat(result.get(0).programTitle()).isEqualTo("Music Track");
        assertThat(result.get(0).instructor()).isNull();
        assertThat(result.get(1).courseTitle()).isEqualTo("Piano Basics");
        assertThat(result.get(1).instructor().displayName()).isEqualTo("Jane Doe");
        assertThat(result.get(1).maxParticipants()).isEqualTo(20);
        verify(classDefinitionRepository, times(1)).findByUuidIn(anyCollection());
        verify(courseInfoService, times(1)).getCourseNames(anyCollection());
        verify(instructorLookupService, times(1)).findInstructorDirectoryEntries(anyCollection());
    }

    @Test
    void hidesEnrolmentFiguresFromCallersWhoAreNotPartyToTheClass() {
        ClassDefinition definition = publicClass(null, null, UUID.randomUUID(), UUID.randomUUID());
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(List.of(definition));

        List<ClassBatchSummaryDTO> result = service.findByUuids(List.of(definition.getUuid()));

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.enrolledCount()).isNull();
            assertThat(row.seatsRemaining()).isNull();
        });
        verify(timetableService, never()).getActiveEnrolmentCounts(any());
    }

    @Test
    void showsEnrolmentFiguresOnlyOnTheCallingInstructorsOwnClasses() {
        UUID me = UUID.randomUUID();
        ClassDefinition mine = publicClass(null, null, me, UUID.randomUUID());
        ClassDefinition theirs = publicClass(null, null, UUID.randomUUID(), UUID.randomUUID());
        when(domainSecurityService.getCurrentInstructorUuid()).thenReturn(me);
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(List.of(mine, theirs));
        when(timetableService.getActiveEnrolmentCounts(List.of(mine.getUuid())))
                .thenReturn(Map.of(mine.getUuid(), 25L));

        List<ClassBatchSummaryDTO> result = service.findByUuids(List.of(mine.getUuid(), theirs.getUuid()));

        assertThat(result.get(0).enrolledCount()).isEqualTo(25L);
        assertThat(result.get(0).seatsRemaining()).isZero();
        assertThat(result.get(1).enrolledCount()).isNull();
        verify(timetableService, times(1)).getActiveEnrolmentCounts(any());
    }

    @Test
    void omitsClassesTheListingScopeDoesNotAdmit() {
        ClassDefinition hidden = publicClass(null, null, null, UUID.randomUUID());
        hidden.setClassVisibility(ClassVisibility.PRIVATE);
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(List.of(hidden));

        assertThat(service.findByUuids(List.of(hidden.getUuid()))).isEmpty();
        verifyNoInteractions(courseInfoService, instructorLookupService, timetableService);
    }

    @Test
    void rejectsBatchesLargerThanTheCapBeforeReading() {
        List<UUID> tooMany = IntStream.rangeClosed(0, ClassBatchLookupService.MAX_BATCH_SIZE)
                .mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> service.findByUuids(tooMany)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(classDefinitionRepository);
    }

    @Test
    void emptyRequestReadsNothing() {
        assertThat(service.findByUuids(List.of())).isEmpty();
        verifyNoInteractions(classDefinitionRepository);
    }

    private static ClassDefinition publicClass(UUID courseUuid, UUID programUuid, UUID instructorUuid,
                                               UUID organisationUuid) {
        ClassDefinition definition = new ClassDefinition();
        definition.setUuid(UUID.randomUUID());
        definition.setTitle("Class " + definition.getUuid());
        definition.setCourseUuid(courseUuid);
        definition.setProgramUuid(programUuid);
        definition.setDefaultInstructorUuid(instructorUuid);
        definition.setOrganisationUuid(organisationUuid);
        definition.setIsActive(true);
        definition.setClassVisibility(ClassVisibility.PUBLIC);
        definition.setMaxParticipants(20);
        return definition;
    }
}
