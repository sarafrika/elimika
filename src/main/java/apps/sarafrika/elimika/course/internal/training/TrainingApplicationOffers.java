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
import apps.sarafrika.elimika.resourcing.spi.ResourceListing;
import apps.sarafrika.elimika.resourcing.spi.ResourceLookupService;
import apps.sarafrika.elimika.resourcing.spi.ResourceType;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What an applicant offers besides rates: venues (organisations only) and answers to the training requirements. */
@Component
@RequiredArgsConstructor
public class TrainingApplicationOffers {

    private final TrainingApplicationVenueRepository venueRepository;
    private final TrainingApplicationRequirementAnswerRepository answerRepository;
    private final CourseTrainingRequirementRepository requirementRepository;
    private final ResourceLookupService resourceLookupService;
    private final TrainingBranchLookupService branchLookupService;

    /** The distinct venues to store, or null when none were sent; each must be an active venue of the applicant organisation. */
    public List<UUID> validateVenues(CourseTrainingApplicantType applicantType, UUID applicantUuid, List<UUID> venueUuids) {
        if (venueUuids == null || venueUuids.isEmpty()) {
            return venueUuids == null ? null : List.of();
        }
        if (venueUuids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("offered_venue_uuids must not contain null");
        }
        if (!CourseTrainingApplicantType.ORGANISATION.equals(applicantType)) {
            throw new IllegalArgumentException("Only organisation applicants can offer venues");
        }
        List<UUID> distinct = venueUuids.stream().distinct().toList();
        Map<UUID, ResourceListing> resources = resourceLookupService.findResources(distinct).stream()
                .collect(Collectors.toMap(ResourceListing::uuid, Function.identity(), (first, second) -> first));
        for (UUID venueUuid : distinct) {
            ResourceListing resource = resources.get(venueUuid);
            if (resource == null) {
                throw new IllegalArgumentException(String.format("Venue %s was not found", venueUuid));
            }
            if (!Objects.equals(applicantUuid, resource.organisationUuid())) {
                throw new IllegalArgumentException(String.format(
                        "Venue %s does not belong to the applicant organisation", venueUuid));
            }
            if (resource.resourceType() != ResourceType.VENUE) {
                throw new IllegalArgumentException(String.format("Resource %s is not a venue", venueUuid));
            }
            if (!resource.active()) {
                throw new IllegalArgumentException(String.format("Venue %s is not active", venueUuid));
            }
        }
        return distinct;
    }

    /** Each answer must name a training requirement of one of {@code courseUuids}, once, with an acquisition when missing. */
    public void validateAnswers(Collection<UUID> courseUuids, List<TrainingRequirementAnswerRequest> answers, String scope) {
        if (answers == null || answers.isEmpty()) {
            return;
        }
        Set<UUID> requirements = courseUuids == null || courseUuids.isEmpty()
                ? Set.of()
                : requirementRepository.findByCourseUuidIn(courseUuids).stream()
                        .map(CourseTrainingRequirement::getUuid)
                        .collect(Collectors.toSet());
        Set<UUID> answered = new HashSet<>();
        for (TrainingRequirementAnswerRequest answer : answers) {
            if (answer == null || answer.requirementUuid() == null || answer.hasIt() == null) {
                throw new IllegalArgumentException("Each requirement answer needs a requirement_uuid and has_it");
            }
            UUID requirementUuid = answer.requirementUuid();
            if (!answered.add(requirementUuid)) {
                throw new IllegalArgumentException(String.format("Requirement %s is answered more than once", requirementUuid));
            }
            if (!requirements.contains(requirementUuid)) {
                throw new IllegalArgumentException(String.format(
                        "Requirement %s is not a training requirement of %s", requirementUuid, scope));
            }
            if (!answer.hasIt() && answer.acquisition() == null) {
                throw new IllegalArgumentException(String.format(
                        "acquisition (lease or hire) is required for requirement %s because has_it is false", requirementUuid));
            }
        }
    }

    /** Replaces the stored venues; null leaves them as they are. */
    public void replaceVenues(TrainingApplicationType type, UUID applicationUuid, List<UUID> venueUuids) {
        if (venueUuids == null) {
            return;
        }
        venueRepository.deleteForApplication(type, applicationUuid);
        venueRepository.saveAll(venueUuids.stream().map(resourceUuid -> {
            TrainingApplicationVenue venue = new TrainingApplicationVenue();
            venue.setApplicationType(type);
            venue.setApplicationUuid(applicationUuid);
            venue.setResourceUuid(resourceUuid);
            return venue;
        }).toList());
    }

    /** Replaces the stored answers; null leaves them as they are. */
    public void replaceAnswers(TrainingApplicationType type, UUID applicationUuid, List<TrainingRequirementAnswerRequest> answers) {
        if (answers == null) {
            return;
        }
        answerRepository.deleteForApplication(type, applicationUuid);
        answerRepository.saveAll(answers.stream().map(request -> {
            TrainingApplicationRequirementAnswer answer = new TrainingApplicationRequirementAnswer();
            answer.setApplicationType(type);
            answer.setApplicationUuid(applicationUuid);
            answer.setRequirementUuid(request.requirementUuid());
            answer.setHasIt(request.hasIt());
            answer.setAcquisition(Boolean.TRUE.equals(request.hasIt()) ? null : request.acquisition());
            return answer;
        }).toList());
    }

    public void deleteFor(TrainingApplicationType type, UUID applicationUuid) {
        venueRepository.deleteForApplication(type, applicationUuid);
        answerRepository.deleteForApplication(type, applicationUuid);
    }

    public Map<UUID, List<TrainingApplicationVenueDTO>> venues(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        List<TrainingApplicationVenue> rows =
                venueRepository.findByApplicationTypeAndApplicationUuidInOrderByIdAsc(type, applicationUuids);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ResourceListing> resources = resourceLookupService
                .findResources(rows.stream().map(TrainingApplicationVenue::getResourceUuid).distinct().toList()).stream()
                .collect(Collectors.toMap(ResourceListing::uuid, Function.identity(), (first, second) -> first));
        Map<UUID, String> branchNames = branchLookupService.findBranchNames(resources.values().stream()
                .map(ResourceListing::branchUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<UUID, List<TrainingApplicationVenueDTO>> venues = new HashMap<>();
        for (TrainingApplicationVenue row : rows) {
            ResourceListing resource = resources.get(row.getResourceUuid());
            venues.computeIfAbsent(row.getApplicationUuid(), uuid -> new ArrayList<>()).add(resource == null
                    ? new TrainingApplicationVenueDTO(row.getResourceUuid(), null, null, null, null, null)
                    : new TrainingApplicationVenueDTO(row.getResourceUuid(), resource.name(), resource.seatCapacity(),
                            resource.locationName(), resource.branchUuid(), branchNames.get(resource.branchUuid())));
        }
        return venues;
    }

    public Map<UUID, List<TrainingRequirementAnswerDTO>> answers(TrainingApplicationType type, Collection<UUID> applicationUuids) {
        List<TrainingApplicationRequirementAnswer> rows =
                answerRepository.findByApplicationTypeAndApplicationUuidInOrderByIdAsc(type, applicationUuids);
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> requirementNames = requirementRepository
                .findByUuidIn(rows.stream().map(TrainingApplicationRequirementAnswer::getRequirementUuid).distinct().toList())
                .stream()
                .collect(Collectors.toMap(CourseTrainingRequirement::getUuid, CourseTrainingRequirement::getName,
                        (first, second) -> first));

        Map<UUID, List<TrainingRequirementAnswerDTO>> answers = new HashMap<>();
        for (TrainingApplicationRequirementAnswer row : rows) {
            answers.computeIfAbsent(row.getApplicationUuid(), uuid -> new ArrayList<>()).add(new TrainingRequirementAnswerDTO(
                    row.getRequirementUuid(),
                    requirementNames.get(row.getRequirementUuid()),
                    Boolean.TRUE.equals(row.getHasIt()),
                    row.getAcquisition()));
        }
        return answers;
    }
}
