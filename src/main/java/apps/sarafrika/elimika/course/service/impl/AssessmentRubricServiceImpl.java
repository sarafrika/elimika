package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.AssessmentRubricDTO;
import apps.sarafrika.elimika.course.factory.AssessmentRubricFactory;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchRouter;
import apps.sarafrika.elimika.course.internal.search.CatalogueSearchScopes;
import apps.sarafrika.elimika.course.internal.search.RubricSearchSource;
import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.service.AssessmentRubricService;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.LikePatterns;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentRubricServiceImpl implements AssessmentRubricService {

    private final AssessmentRubricRepository assessmentRubricRepository;
    private final GenericSpecificationBuilder<AssessmentRubric> specificationBuilder;
    private final DomainSecurityService domainSecurityService;
    private final CatalogueSearchRouter catalogueSearchRouter;

    /** How rubric list params map onto the {@code rubrics} index. */
    private static final CatalogueSearchRouter.Route RUBRIC_SEARCH_ROUTE = new CatalogueSearchRouter.Route(
            RubricSearchSource.DEFINITION,
            Map.of("createddate", "created_at", "createdat", "created_at"),
            Set.of("status"));

    private static final String ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE = "Assessment rubric with ID %s not found";

    @Override
    public AssessmentRubricDTO createAssessmentRubric(AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubric assessmentRubric = AssessmentRubricFactory.toEntity(assessmentRubricDTO);

        // Set defaults based on AssessmentRubricDTO business logic
        if (assessmentRubric.getStatus() == null) {
            assessmentRubric.setStatus(ContentStatus.DRAFT);
        }
        if (assessmentRubric.getIsActive() == null) {
            assessmentRubric.setIsActive(false);
        }
        if (assessmentRubric.getIsPublic() == null) {
            assessmentRubric.setIsPublic(false);
        }
        if (assessmentRubric.getTotalWeight() == null) {
            assessmentRubric.setTotalWeight(new java.math.BigDecimal("100.00"));
        }
        if (assessmentRubric.getWeightUnit() == null) {
            assessmentRubric.setWeightUnit("percentage");
        }
        if (assessmentRubric.getUsesCustomLevels() == null) {
            assessmentRubric.setUsesCustomLevels(true);
        }

        AssessmentRubric savedAssessmentRubric = assessmentRubricRepository.save(assessmentRubric);
        return AssessmentRubricFactory.toDTO(savedAssessmentRubric);
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentRubricDTO getAssessmentRubricByUuid(UUID uuid) {
        return assessmentRubricRepository.findByUuid(uuid)
                .map(AssessmentRubricFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricDTO> getAllAssessmentRubrics(Pageable pageable) {
        specificationBuilder.validateSortProperties(AssessmentRubric.class, pageable);
        return assessmentRubricRepository.findAll(pageable).map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public AssessmentRubricDTO updateAssessmentRubric(UUID uuid, AssessmentRubricDTO assessmentRubricDTO) {
        AssessmentRubric existingAssessmentRubric = assessmentRubricRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid)));

        updateAssessmentRubricFields(existingAssessmentRubric, assessmentRubricDTO);

        AssessmentRubric updatedAssessmentRubric = assessmentRubricRepository.save(existingAssessmentRubric);
        return AssessmentRubricFactory.toDTO(updatedAssessmentRubric);
    }

    @Override
    public void deleteAssessmentRubric(UUID uuid) {
        if (!assessmentRubricRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(ASSESSMENT_RUBRIC_NOT_FOUND_TEMPLATE, uuid));
        }
        assessmentRubricRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricDTO> search(Map<String, String> searchParams, Pageable pageable) {
        specificationBuilder.validateSortProperties(AssessmentRubric.class, pageable);
        Specification<AssessmentRubric> spec = specificationBuilder.buildSpecification(
                AssessmentRubric.class, CatalogueSearchRouter.forDatabase(searchParams, "title_like"));
        return assessmentRubricRepository.findAll(spec, pageable).map(AssessmentRubricFactory::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssessmentRubricDTO> searchForCaller(Map<String, String> searchParams, Pageable pageable) {
        boolean platformAdmin = domainSecurityService.isPlatformAdmin();
        String q = CatalogueSearchRouter.queryText(searchParams);
        if (q != null) {
            UUID courseCreatorUuid = platformAdmin ? null : domainSecurityService.getCurrentCourseCreatorUuid();
            Optional<Page<AssessmentRubricDTO>> found = catalogueSearchRouter.search(RUBRIC_SEARCH_ROUTE, q,
                    searchParams, pageable,
                    () -> CatalogueSearchScopes.rubrics(platformAdmin, courseCreatorUuid),
                    uuids -> hydrate(uuids, platformAdmin ? null : visibleTo(courseCreatorUuid)),
                    AssessmentRubricDTO::uuid);
            if (found.isPresent()) {
                return found.get();
            }
        }
        if (platformAdmin) {
            return search(searchParams, pageable);
        }
        specificationBuilder.validateSortProperties(AssessmentRubric.class, pageable);
        Specification<AssessmentRubric> spec = specificationBuilder.buildSpecification(
                AssessmentRubric.class, CatalogueSearchRouter.forDatabase(searchParams, "title_like"));
        Specification<AssessmentRubric> visible = visibleToCaller();
        spec = spec == null ? visible : spec.and(visible);
        return assessmentRubricRepository.findAll(spec, pageable).map(AssessmentRubricFactory::toDTO);
    }

    /**
     * Public rubrics, plus the ones the caller authored as a course creator.
     */
    private Specification<AssessmentRubric> visibleToCaller() {
        return visibleTo(domainSecurityService.getCurrentCourseCreatorUuid());
    }

    private static Specification<AssessmentRubric> visibleTo(UUID courseCreatorUuid) {
        return (root, query, cb) -> courseCreatorUuid == null
                ? cb.isTrue(root.get("isPublic"))
                : cb.or(cb.isTrue(root.get("isPublic")), cb.equal(root.get("courseCreatorUuid"), courseCreatorUuid));
    }

    private static Specification<AssessmentRubric> publicAndActive() {
        return (root, query, cb) -> cb.and(cb.isTrue(root.get("isPublic")), cb.isTrue(root.get("isActive")));
    }

    /**
     * Loads the search hits in one batch query that re-applies the caller's SQL visibility, so a hit
     * from a document that has not caught up with the database yet is never shown.
     */
    private List<AssessmentRubricDTO> hydrate(List<UUID> uuids, Specification<AssessmentRubric> visible) {
        Specification<AssessmentRubric> byUuid = (root, query, cb) -> root.get("uuid").in(uuids);
        return assessmentRubricRepository.findAll(visible == null ? byUuid : byUuid.and(visible)).stream()
                .map(AssessmentRubricFactory::toDTO)
                .toList();
    }

    private void updateAssessmentRubricFields(AssessmentRubric existingAssessmentRubric, AssessmentRubricDTO dto) {
        if (dto.title() != null) {
            existingAssessmentRubric.setTitle(dto.title());
        }
        if (dto.description() != null) {
            existingAssessmentRubric.setDescription(dto.description());
        }
        if (dto.rubricType() != null) {
            existingAssessmentRubric.setRubricType(dto.rubricType());
        }
        if (dto.courseCreatorUuid() != null) {
            existingAssessmentRubric.setCourseCreatorUuid(dto.courseCreatorUuid());
        }
        if (dto.isPublic() != null) {
            existingAssessmentRubric.setIsPublic(dto.isPublic());
        }
        if (dto.status() != null) {
            existingAssessmentRubric.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingAssessmentRubric.setIsActive(dto.active());
        }
        if (dto.totalWeight() != null) {
            existingAssessmentRubric.setTotalWeight(dto.totalWeight());
        }
        if (dto.weightUnit() != null) {
            existingAssessmentRubric.setWeightUnit(dto.weightUnit());
        }
        if (dto.usesCustomLevels() != null) {
            existingAssessmentRubric.setUsesCustomLevels(dto.usesCustomLevels());
        }
        if (dto.maxScore() != null) {
            existingAssessmentRubric.setMaxScore(dto.maxScore());
        }
        if (dto.minPassingScore() != null) {
            existingAssessmentRubric.setMinPassingScore(dto.minPassingScore());
        }
    }

    @Override
    public Page<AssessmentRubricDTO> getPublicRubrics(Pageable pageable) {
        return assessmentRubricRepository.findByIsPublicTrueAndIsActiveTrueOrderByCreatedDateDesc(pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> searchPublicRubrics(String searchTerm, String rubricType, Pageable pageable) {
        // A type filter is a case-insensitive substring match the index cannot express, so only a
        // plain text search goes to search; everything else keeps the database queries below.
        if (searchTerm != null && rubricType == null) {
            Optional<Page<AssessmentRubricDTO>> found = catalogueSearchRouter.search(RUBRIC_SEARCH_ROUTE,
                    searchTerm, Map.of(), pageable,
                    CatalogueSearchScopes::publicRubrics,
                    uuids -> hydrate(uuids, publicAndActive()),
                    AssessmentRubricDTO::uuid);
            if (found.isPresent()) {
                return found.get();
            }
        }
        if (searchTerm != null && rubricType != null) {
            // Search with both term and type
            return assessmentRubricRepository.findPublicRubricsBySearchTermAndType(
                            LikePatterns.escapeLower(searchTerm), LikePatterns.escapeLower(rubricType), pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else if (searchTerm != null) {
            // Search by term only
            return assessmentRubricRepository.findPublicRubricsBySearchTerm(LikePatterns.escapeLower(searchTerm), pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else if (rubricType != null) {
            // Filter by type only
            return assessmentRubricRepository.findByIsPublicTrueAndIsActiveTrueAndRubricTypeContainingIgnoreCaseOrderByCreatedDateDesc(rubricType, pageable)
                    .map(AssessmentRubricFactory::toDTO);
        } else {
            // No filters - return all public rubrics
            return getPublicRubrics(pageable);
        }
    }

    @Override
    public Page<AssessmentRubricDTO> getCourseCreatorRubrics(UUID courseCreatorUuid, boolean includePrivate, Pageable pageable) {
        // Private rubrics belong to their author: anyone else asking for them gets the public ones.
        boolean mayIncludePrivate = includePrivate
                && (domainSecurityService.isPlatformAdmin()
                        || domainSecurityService.isCourseCreatorWithUuid(courseCreatorUuid));
        return assessmentRubricRepository.findCourseCreatorShareableRubrics(courseCreatorUuid, mayIncludePrivate, pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> getGeneralRubrics(Pageable pageable) {
        // All rubrics are now general-use and can be associated with multiple courses
        return getPublicRubrics(pageable);
    }

    @Override
    public Page<AssessmentRubricDTO> getPopularRubrics(Pageable pageable) {
        return assessmentRubricRepository.findPopularPublicRubrics(pageable)
                .map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Page<AssessmentRubricDTO> getRubricsByStatus(ContentStatus status, Pageable pageable) {
        Page<AssessmentRubric> rubrics = domainSecurityService.isPlatformAdmin()
                ? assessmentRubricRepository.findByStatusAndIsActiveTrueOrderByCreatedDateDesc(status, pageable)
                : assessmentRubricRepository.findByStatusAndIsPublicTrueAndIsActiveTrueOrderByCreatedDateDesc(status, pageable);
        return rubrics.map(AssessmentRubricFactory::toDTO);
    }

    @Override
    public Map<String, Long> getRubricStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalPublicRubrics", assessmentRubricRepository.countByIsPublicTrueAndIsActiveTrue());
        stats.put("totalRubrics", assessmentRubricRepository.count());
        // Add more statistics as needed
        return stats;
    }

    @Override
    public Map<String, Long> getCourseCreatorRubricStatistics(UUID courseCreatorUuid) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalRubrics", assessmentRubricRepository.countByCourseCreatorUuidAndIsActiveTrue(courseCreatorUuid));
        // Add more course creator-specific statistics as needed
        return stats;
    }
}
