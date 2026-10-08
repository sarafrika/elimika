package apps.sarafrika.elimika.course.internal.agegroup;

import apps.sarafrika.elimika.course.dto.SavedAgeGroupDTO;
import apps.sarafrika.elimika.course.dto.SavedAgeGroupRequest;
import apps.sarafrika.elimika.course.model.AgeGroup;
import apps.sarafrika.elimika.course.repository.AgeGroupRepository;
import apps.sarafrika.elimika.course.util.enums.AgeGroupOwnerType;
import apps.sarafrika.elimika.shared.exceptions.DuplicateResourceException;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The age groups instructors and organisations keep for reuse. Applications copy a saved group
 * (name and ages) and never link back, so editing or deleting one here changes no application.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SavedAgeGroupService {

    static final int MAX_SAVED = 50;

    private final AgeGroupRepository repository;
    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;
    private final OrganisationLookupService organisationLookupService;

    @Transactional(readOnly = true)
    public List<SavedAgeGroupDTO> listMine() {
        return list(AgeGroupOwnerType.INSTRUCTOR, currentInstructor(), null);
    }

    public SavedAgeGroupDTO createMine(SavedAgeGroupRequest request) {
        return create(AgeGroupOwnerType.INSTRUCTOR, currentInstructor(), request);
    }

    @Transactional(readOnly = true)
    public List<SavedAgeGroupDTO> listForOrganisation(UUID organisationUuid) {
        return list(AgeGroupOwnerType.ORGANISATION, organisationUuid, organisationName(organisationUuid));
    }

    public SavedAgeGroupDTO createForOrganisation(UUID organisationUuid, SavedAgeGroupRequest request) {
        return create(AgeGroupOwnerType.ORGANISATION, organisationUuid, request);
    }

    /** The caller's own groups first, then those of every organisation they currently belong to. */
    @Transactional(readOnly = true)
    public List<SavedAgeGroupDTO> presetsForCurrentInstructor() {
        List<SavedAgeGroupDTO> presets = new ArrayList<>(listMine());
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        List<UUID> organisations = userUuid == null ? List.of() : userLookupService.getActiveUserOrganizations(userUuid);
        if (organisations.isEmpty()) {
            return presets;
        }
        Map<UUID, String> names = organisationLookupService.findOrganisationNames(organisations);
        repository.findByOwnerTypeAndOwnerUuidInOrderByPositionAscIdAsc(AgeGroupOwnerType.ORGANISATION, organisations)
                .forEach(group -> presets.add(toDTO(group, names.get(group.getOwnerUuid()))));
        return presets;
    }

    public SavedAgeGroupDTO update(UUID ageGroupUuid, SavedAgeGroupRequest request) {
        AgeGroup group = ownedSavedGroup(ageGroupUuid);
        validate(group.getOwnerType(), group.getOwnerUuid(), request, group.getUuid());
        group.setName(request.name().trim());
        group.setMinAge(request.minAge());
        group.setMaxAge(request.maxAge());
        return toDTO(repository.save(group), ownerName(group));
    }

    public void delete(UUID ageGroupUuid) {
        repository.delete(ownedSavedGroup(ageGroupUuid));
    }

    private List<SavedAgeGroupDTO> list(AgeGroupOwnerType ownerType, UUID ownerUuid, String ownerName) {
        return repository.findByOwnerTypeAndOwnerUuidInOrderByPositionAscIdAsc(ownerType, List.of(ownerUuid)).stream()
                .map(group -> toDTO(group, ownerName))
                .toList();
    }

    private SavedAgeGroupDTO create(AgeGroupOwnerType ownerType, UUID ownerUuid, SavedAgeGroupRequest request) {
        validate(ownerType, ownerUuid, request, null);
        long count = repository.countByOwnerTypeAndOwnerUuid(ownerType, ownerUuid);
        if (count >= MAX_SAVED) {
            throw new IllegalArgumentException(String.format("At most %d saved age groups are allowed", MAX_SAVED));
        }
        AgeGroup group = new AgeGroup();
        group.setOwnerType(ownerType);
        group.setOwnerUuid(ownerUuid);
        group.setName(request.name().trim());
        group.setMinAge(request.minAge());
        group.setMaxAge(request.maxAge());
        group.setPosition((int) count);
        return toDTO(repository.save(group), ownerName(group));
    }

    /** Overlaps are fine here: an owner may keep several schemes side by side. */
    private void validate(AgeGroupOwnerType ownerType, UUID ownerUuid, SavedAgeGroupRequest request, UUID self) {
        if (request.minAge() > request.maxAge()) {
            throw new IllegalArgumentException("min_age must not be above max_age");
        }
        String name = request.name().trim().toLowerCase(Locale.ROOT);
        boolean taken = repository.findByOwnerTypeAndOwnerUuidInOrderByPositionAscIdAsc(ownerType, List.of(ownerUuid))
                .stream()
                .anyMatch(group -> !group.getUuid().equals(self) && group.getName().toLowerCase(Locale.ROOT).equals(name));
        if (taken) {
            throw new DuplicateResourceException(String.format("An age group named '%s' already exists", request.name().trim()));
        }
    }

    /** Only saved groups are reachable here, and only by their owner; others read as not found. */
    private AgeGroup ownedSavedGroup(UUID ageGroupUuid) {
        AgeGroup group = repository.findByUuid(ageGroupUuid)
                .filter(found -> found.getOwnerType().isSaved())
                .orElseThrow(() -> new ResourceNotFoundException("Age group " + ageGroupUuid + " was not found"));
        boolean owner = switch (group.getOwnerType()) {
            case INSTRUCTOR -> group.getOwnerUuid().equals(domainSecurityService.getCurrentInstructorUuid());
            case ORGANISATION -> domainSecurityService.managesOrganisation(group.getOwnerUuid());
            default -> false;
        };
        if (!owner) {
            throw new AccessDeniedException("You can only change your own age groups");
        }
        return group;
    }

    private UUID currentInstructor() {
        UUID instructorUuid = domainSecurityService.getCurrentInstructorUuid();
        if (instructorUuid == null) {
            throw new AccessDeniedException("Only instructors keep personal age groups");
        }
        return instructorUuid;
    }

    private String ownerName(AgeGroup group) {
        return group.getOwnerType() == AgeGroupOwnerType.ORGANISATION ? organisationName(group.getOwnerUuid()) : null;
    }

    private String organisationName(UUID organisationUuid) {
        return organisationLookupService.findOrganisationNames(List.of(organisationUuid)).get(organisationUuid);
    }

    private static SavedAgeGroupDTO toDTO(AgeGroup group, String ownerName) {
        return new SavedAgeGroupDTO(group.getUuid(), group.getName(), group.getMinAge(), group.getMaxAge(),
                group.getOwnerType(), group.getOwnerUuid(), ownerName);
    }
}
