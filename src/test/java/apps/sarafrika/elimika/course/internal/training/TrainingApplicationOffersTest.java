package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.TrainingApplicationVenueDTO;
import apps.sarafrika.elimika.course.dto.TrainingRequirementAnswerDTO;
import apps.sarafrika.elimika.course.dto.TrainingRequirementAnswerRequest;
import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import apps.sarafrika.elimika.course.model.TrainingApplicationRequirementAnswer;
import apps.sarafrika.elimika.course.model.TrainingApplicationVenue;
import apps.sarafrika.elimika.course.repository.CourseTrainingRequirementRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationRequirementAnswerRepository;
import apps.sarafrika.elimika.course.repository.TrainingApplicationVenueRepository;
import apps.sarafrika.elimika.course.util.enums.CourseTrainingApplicantType;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRequirementAcquisition;
import apps.sarafrika.elimika.resourcing.spi.ResourceListing;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingApplicationOffersTest {

    @Mock private TrainingApplicationVenueRepository venueRepository;
    @Mock private TrainingApplicationRequirementAnswerRepository answerRepository;
    @Mock private CourseTrainingRequirementRepository requirementRepository;
    @Mock private ResourceLookupService resourceLookupService;
    @Mock private TrainingBranchLookupService branchLookupService;

    private TrainingApplicationOffers offers;

    private final UUID organisationUuid = UUID.randomUUID();
    private final UUID courseUuid = UUID.randomUUID();
    private final UUID branchUuid = UUID.randomUUID();
    private final UUID venueUuid = UUID.randomUUID();
    private final UUID requirementUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        offers = new TrainingApplicationOffers(venueRepository, answerRepository, requirementRepository,
                resourceLookupService, branchLookupService);
        CourseTrainingRequirement requirement = new CourseTrainingRequirement();
        requirement.setUuid(requirementUuid);
        requirement.setCourseUuid(courseUuid);
        requirement.setName("Welding booth");
        when(requirementRepository.findByCourseUuidIn(List.of(courseUuid))).thenReturn(List.of(requirement));
        when(requirementRepository.findByUuidIn(anyCollection())).thenReturn(List.of(requirement));
    }

    // ===== venues =====

    @Test
    @DisplayName("an active venue of the applicant organisation is accepted, once")
    void ownActiveVenueIsAccepted() {
        resources(listing(venueUuid, organisationUuid, ResourceType.VENUE, true));

        assertThat(offers.validateVenues(CourseTrainingApplicantType.ORGANISATION, organisationUuid, List.of(venueUuid, venueUuid)))
                .containsExactly(venueUuid);
    }

    @Test
    @DisplayName("a venue of another organisation is rejected")
    void foreignVenueIsRejected() {
        resources(listing(venueUuid, UUID.randomUUID(), ResourceType.VENUE, true));

        assertThatThrownBy(() -> offers.validateVenues(CourseTrainingApplicantType.ORGANISATION, organisationUuid, List.of(venueUuid)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the applicant organisation");
    }

    @Test
    @DisplayName("an equipment pool is not a venue")
    void nonVenueIsRejected() {
        resources(listing(venueUuid, organisationUuid, ResourceType.EQUIPMENT_POOL, true));

        assertThatThrownBy(() -> offers.validateVenues(CourseTrainingApplicantType.ORGANISATION, organisationUuid, List.of(venueUuid)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not a venue");
    }

    @Test
    @DisplayName("inactive and unknown venues are rejected")
    void inactiveAndUnknownVenuesAreRejected() {
        resources(listing(venueUuid, organisationUuid, ResourceType.VENUE, false));
        assertThatThrownBy(() -> offers.validateVenues(CourseTrainingApplicantType.ORGANISATION, organisationUuid, List.of(venueUuid)))
                .hasMessageContaining("is not active");

        resources();
        assertThatThrownBy(() -> offers.validateVenues(CourseTrainingApplicantType.ORGANISATION, organisationUuid, List.of(venueUuid)))
                .hasMessageContaining("was not found");
    }

    @Test
    @DisplayName("instructors cannot offer venues, but may send an empty list")
    void instructorsOfferNoVenues() {
        assertThatThrownBy(() -> offers.validateVenues(CourseTrainingApplicantType.INSTRUCTOR, UUID.randomUUID(), List.of(venueUuid)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only organisation applicants");
        assertThat(offers.validateVenues(CourseTrainingApplicantType.INSTRUCTOR, UUID.randomUUID(), List.of())).isEmpty();
        assertThat(offers.validateVenues(CourseTrainingApplicantType.INSTRUCTOR, UUID.randomUUID(), null)).isNull();
    }

    // ===== requirement answers =====

    @Test
    @DisplayName("an answer to a requirement of another course is rejected")
    void unknownRequirementIsRejected() {
        UUID elsewhere = UUID.randomUUID();
        assertThatThrownBy(() -> offers.validateAnswers(List.of(courseUuid),
                List.of(new TrainingRequirementAnswerRequest(elsewhere, true, null)), "this course"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(elsewhere.toString())
                .hasMessageContaining("this course");
    }

    @Test
    @DisplayName("a missing requirement needs to say how it would be obtained")
    void missingAcquisitionIsRejected() {
        assertThatThrownBy(() -> offers.validateAnswers(List.of(courseUuid),
                List.of(new TrainingRequirementAnswerRequest(requirementUuid, false, null)), "this course"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acquisition");
    }

    @Test
    @DisplayName("a requirement answered twice is rejected")
    void duplicateAnswerIsRejected() {
        assertThatThrownBy(() -> offers.validateAnswers(List.of(courseUuid), List.of(
                new TrainingRequirementAnswerRequest(requirementUuid, true, null),
                new TrainingRequirementAnswerRequest(requirementUuid, false, TrainingRequirementAcquisition.HIRE)), "this course"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than once");
    }

    @Test
    @DisplayName("answers are stored with the acquisition only when the requirement is missing")
    void answersAreStored() {
        UUID applicationUuid = UUID.randomUUID();
        offers.validateAnswers(List.of(courseUuid),
                List.of(new TrainingRequirementAnswerRequest(requirementUuid, true, TrainingRequirementAcquisition.LEASE)), "this course");

        offers.replaceAnswers(TrainingApplicationType.COURSE, applicationUuid,
                List.of(new TrainingRequirementAnswerRequest(requirementUuid, true, TrainingRequirementAcquisition.LEASE)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrainingApplicationRequirementAnswer>> saved = ArgumentCaptor.forClass(List.class);
        verify(answerRepository).deleteForApplication(TrainingApplicationType.COURSE, applicationUuid);
        verify(answerRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(answer -> {
            assertThat(answer.getHasIt()).isTrue();
            assertThat(answer.getAcquisition()).isNull();
        });
    }

    @Test
    @DisplayName("replacing with null keeps what is stored")
    void nullKeepsStoredOffers() {
        offers.replaceVenues(TrainingApplicationType.PROGRAM, UUID.randomUUID(), null);
        offers.replaceAnswers(TrainingApplicationType.PROGRAM, UUID.randomUUID(), null);

        verify(venueRepository, never()).deleteForApplication(any(), any());
        verify(answerRepository, never()).deleteForApplication(any(), any());
    }

    // ===== reading =====

    @Test
    @DisplayName("stored venues and answers come back resolved in batch")
    void storedOffersAreResolved() {
        UUID applicationUuid = UUID.randomUUID();
        TrainingApplicationVenue venue = new TrainingApplicationVenue();
        venue.setApplicationUuid(applicationUuid);
        venue.setResourceUuid(venueUuid);
        when(venueRepository.findByApplicationTypeAndApplicationUuidInOrderByIdAsc(eq(TrainingApplicationType.COURSE), anyCollection()))
                .thenReturn(List.of(venue));
        resources(listing(venueUuid, organisationUuid, ResourceType.VENUE, true));
        when(branchLookupService.findBranchNames(anyCollection())).thenReturn(Map.of(branchUuid, "Westlands"));

        TrainingApplicationRequirementAnswer answer = new TrainingApplicationRequirementAnswer();
        answer.setApplicationUuid(applicationUuid);
        answer.setRequirementUuid(requirementUuid);
        answer.setHasIt(false);
        answer.setAcquisition(TrainingRequirementAcquisition.HIRE);
        when(answerRepository.findByApplicationTypeAndApplicationUuidInOrderByIdAsc(eq(TrainingApplicationType.COURSE), anyCollection()))
                .thenReturn(List.of(answer));

        List<TrainingApplicationVenueDTO> venues = offers.venues(TrainingApplicationType.COURSE, List.of(applicationUuid)).get(applicationUuid);
        List<TrainingRequirementAnswerDTO> answers = offers.answers(TrainingApplicationType.COURSE, List.of(applicationUuid)).get(applicationUuid);

        assertThat(venues).containsExactly(new TrainingApplicationVenueDTO(venueUuid, "Lab 1", 30, "Block B", branchUuid, "Westlands"));
        assertThat(answers).containsExactly(new TrainingRequirementAnswerDTO(requirementUuid, "Welding booth", false, TrainingRequirementAcquisition.HIRE));
    }

    private void resources(ResourceListing... listings) {
        when(resourceLookupService.findResources(anyCollection())).thenReturn(List.of(listings));
    }

    private ResourceListing listing(UUID uuid, UUID owner, ResourceType type, boolean active) {
        return new ResourceListing(uuid, owner, branchUuid, type, "Lab 1", type == ResourceType.VENUE ? 30 : null, "Block B", active);
    }
}
