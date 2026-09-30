package apps.sarafrika.elimika.classes.service.impl;

import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsDTO;
import apps.sarafrika.elimika.classes.dto.ClassMarketplaceJobRequiredSkillsRequest;
import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJobRequiredSkill;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRequiredSkillRepository;
import apps.sarafrika.elimika.classes.service.ClassMarketplaceJobRequiredSkillService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.skills.spi.SkillSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Required skills of marketplace jobs. Both reading and writing are limited to managers of the
 * posting organisation and platform admins, the same rule as editing the job itself.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassMarketplaceJobRequiredSkillServiceImpl implements ClassMarketplaceJobRequiredSkillService {

    private final ClassMarketplaceJobRepository jobRepository;
    private final ClassMarketplaceJobRequiredSkillRepository requiredSkillRepository;
    private final JobRequiredSkills jobRequiredSkills;
    private final SkillLookupService skillLookupService;
    private final DomainSecurityService domainSecurityService;

    @Override
    @Transactional(readOnly = true)
    public ClassMarketplaceJobRequiredSkillsDTO getRequiredSkills(UUID jobUuid) {
        ClassMarketplaceJob job = requireManagedJob(jobUuid);
        return toDto(job);
    }

    @Override
    public ClassMarketplaceJobRequiredSkillsDTO replaceRequiredSkills(UUID jobUuid,
                                                                     ClassMarketplaceJobRequiredSkillsRequest request) {
        ClassMarketplaceJob job = requireManagedJob(jobUuid);
        List<ClassMarketplaceJobRequiredSkillsRequest.Item> items = request.skills() == null ? List.of() : request.skills();
        Set<UUID> requested = new HashSet<>();
        for (ClassMarketplaceJobRequiredSkillsRequest.Item item : items) {
            if (!requested.add(item.skillUuid())) {
                throw new IllegalArgumentException("Skill " + item.skillUuid() + " is listed more than once");
            }
        }
        List<ClassMarketplaceJobRequiredSkill> existing = requiredSkillRepository.findByJobUuidOrderByIdAsc(jobUuid);
        Map<UUID, ClassMarketplaceJobRequiredSkill> existingBySkill = existing.stream()
                .collect(Collectors.toMap(ClassMarketplaceJobRequiredSkill::getSkillUuid, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
        validateSkills(requested, existingBySkill.keySet());

        requiredSkillRepository.deleteAll(existing.stream()
                .filter(row -> !requested.contains(row.getSkillUuid()))
                .toList());
        requiredSkillRepository.flush();

        List<ClassMarketplaceJobRequiredSkill> kept = new ArrayList<>();
        for (ClassMarketplaceJobRequiredSkillsRequest.Item item : items) {
            ProficiencyLevel level = item.minProficiency() == null ? ProficiencyLevel.BEGINNER : item.minProficiency();
            boolean mandatory = item.isMandatory() == null || item.isMandatory();
            ClassMarketplaceJobRequiredSkill row = existingBySkill.get(item.skillUuid());
            if (row == null) {
                row = new ClassMarketplaceJobRequiredSkill(jobUuid, item.skillUuid(), level, mandatory);
            } else {
                row.setMinProficiency(level);
                row.setIsMandatory(mandatory);
            }
            kept.add(row);
        }
        requiredSkillRepository.saveAllAndFlush(kept);
        return toDto(job);
    }

    private ClassMarketplaceJob requireManagedJob(UUID jobUuid) {
        ClassMarketplaceJob job = jobRepository.findByUuid(jobUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Marketplace job with UUID " + jobUuid + " not found"));
        if (!domainSecurityService.isPlatformAdmin() && !domainSecurityService.managesOrganisation(job.getOrganisationUuid())) {
            throw new AccessDeniedException("Only the posting organisation's managers or a platform admin may manage "
                    + "this job's required skills");
        }
        return job;
    }

    /** Every skill must exist; a retired one may stay on the job but may not be newly added. */
    private void validateSkills(Set<UUID> requested, Set<UUID> alreadyTagged) {
        if (requested.isEmpty()) {
            return;
        }
        Map<UUID, SkillSummary> found = skillLookupService.findByUuids(requested).stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity()));
        List<UUID> unknown = requested.stream().filter(uuid -> !found.containsKey(uuid)).toList();
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown skill(s): " + unknown);
        }
        List<String> retired = requested.stream()
                .filter(uuid -> !found.get(uuid).active() && !alreadyTagged.contains(uuid))
                .map(uuid -> found.get(uuid).name())
                .toList();
        if (!retired.isEmpty()) {
            throw new IllegalArgumentException("Retired skill(s) cannot be added: " + retired);
        }
    }

    private ClassMarketplaceJobRequiredSkillsDTO toDto(ClassMarketplaceJob job) {
        JobRequiredSkills.Effective effective = jobRequiredSkills.forJob(job);
        Map<UUID, SkillSummary> skills = effective.tags().isEmpty() ? Map.of()
                : skillLookupService.findByUuids(effective.tags().stream().map(JobRequiredSkills.Tag::skillUuid).toList())
                .stream()
                .collect(Collectors.toMap(SkillSummary::uuid, Function.identity()));
        List<ClassMarketplaceJobRequiredSkillsDTO.Skill> dtos = new ArrayList<>();
        for (JobRequiredSkills.Tag tag : effective.tags()) {
            SkillSummary skill = skills.get(tag.skillUuid());
            if (skill == null) {
                continue;
            }
            dtos.add(new ClassMarketplaceJobRequiredSkillsDTO.Skill(skill.uuid(), skill.name(), skill.slug(),
                    tag.minProficiency(), tag.mandatory(), effective.inherited(), skill.active()));
        }
        return new ClassMarketplaceJobRequiredSkillsDTO(job.getUuid(), effective.inherited(),
                effective.inheritedFromCourseUuid(), dtos);
    }
}
