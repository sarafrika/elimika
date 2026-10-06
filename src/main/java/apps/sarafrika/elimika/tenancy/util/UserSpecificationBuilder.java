package apps.sarafrika.elimika.tenancy.util;

import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Custom specification builder for User entity searches.
 * Extends the generic specification builder with domain-specific predicates
 * that handle computed fields and relationship-based searches.
 * <p>
 * Relational filters only. Free text over names and email addresses is served by the {@code people}
 * search index through {@code q}; the former {@code full_name} / {@code full_name_like} keys are gone.
 *
 * @author Wilfred Njuguna
 * @since 2025-10-16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSpecificationBuilder {

    private final GenericSpecificationBuilder<User> genericBuilder;
    private final UserDomainRepository userDomainRepository;

    /**
     * Special search parameter keys that require custom handling
     */
    private static final String PARAM_USER_DOMAIN = "user_domain";
    private static final String PARAM_ORGANISATION_UUID = "organisation_uuid";
    private static final String PARAM_DOMAIN_IN_ORGANISATION = "domain_in_organisation";
    private static final String PARAM_BRANCH_UUID = "branch_uuid";
    private static final String PARAM_ACTIVE_IN_ORGANISATION = "active_in_organisation";

    /**
     * Rejects an ordering on a column that is not exposed for filtering, so that a page of users
     * cannot be ranked by an unexposed column. Delegates to the generic builder's allow-list.
     *
     * @param pageable the bound page request whose sort orders are checked
     * @throws IllegalArgumentException when a sort property is not exposed
     */
    public void validateSortProperties(Pageable pageable) {
        genericBuilder.validateSortProperties(User.class, pageable);
    }

    /**
     * Builds a complete user specification from search parameters.
     * Extracts domain-specific parameters and combines them with generic field searches.
     *
     * @param searchParams Map of search parameters from the request
     * @return Combined specification for User entity queries
     */
    public Specification<User> buildUserSpecification(Map<String, String> searchParams) {
        log.debug("Building user specification from params: {}", searchParams);

        // Create a mutable copy to extract custom params
        Map<String, String> modifiableParams = new HashMap<>(searchParams);

        // Extract domain-specific parameters
        String userDomain = modifiableParams.remove(PARAM_USER_DOMAIN);
        String organisationUuid = modifiableParams.remove(PARAM_ORGANISATION_UUID);
        String domainInOrganisation = modifiableParams.remove(PARAM_DOMAIN_IN_ORGANISATION);
        String branchUuid = modifiableParams.remove(PARAM_BRANCH_UUID);
        String activeInOrg = modifiableParams.remove(PARAM_ACTIVE_IN_ORGANISATION);

        // Build base specification from generic builder
        Specification<User> spec = genericBuilder.buildSpecification(User.class, modifiableParams);

        // Add domain-specific predicates
        if (userDomain != null && !userDomain.isEmpty()) {
            spec = addSpecification(spec, hasUserDomain(userDomain));
        }

        if (organisationUuid != null && !organisationUuid.isEmpty()) {
            spec = addSpecification(spec, belongsToOrganisation(UUID.fromString(organisationUuid), activeInOrg));
        }

        if (domainInOrganisation != null && !domainInOrganisation.isEmpty() &&
                organisationUuid != null && !organisationUuid.isEmpty()) {
            spec = addSpecification(spec, hasDomainInOrganisation(
                    UUID.fromString(organisationUuid), domainInOrganisation));
        }

        if (branchUuid != null && !branchUuid.isEmpty()) {
            spec = addSpecification(spec, belongsToBranch(UUID.fromString(branchUuid)));
        }

        return spec;
    }

    /**
     * Helper to safely combine specifications
     */
    private Specification<User> addSpecification(Specification<User> existing, Specification<User> additional) {
        if (existing == null) {
            return additional;
        }
        return existing.and(additional);
    }

    /**
     * Creates a specification to filter users by domain name.
     * Checks both standalone domains (user_domain_mapping) and organization domains.
     *
     * @param domainName The domain name to search for (e.g., "student", "instructor")
     * @return Specification that checks if user has the specified domain
     */
    public Specification<User> hasUserDomain(String domainName) {
        return (root, query, criteriaBuilder) -> {
            log.debug("Building hasUserDomain predicate for domain: {}", domainName);

            // Subquery to check standalone domains
            Subquery<Long> standaloneDomainSubquery = query.subquery(Long.class);
            Root<UserDomainMapping> mappingRoot = standaloneDomainSubquery.from(UserDomainMapping.class);
            Join<UserDomainMapping, UserDomain> domainJoin = mappingRoot.join("userDomain", JoinType.INNER);

            standaloneDomainSubquery.select(criteriaBuilder.count(mappingRoot.get("id")))
                    .where(
                            criteriaBuilder.equal(mappingRoot.get("userUuid"), root.get("uuid")),
                            criteriaBuilder.equal(
                                    criteriaBuilder.lower(domainJoin.get("domainName")),
                                    domainName.toLowerCase()
                            ),
                            criteriaBuilder.equal(mappingRoot.get("status"), DomainApprovalStatus.APPROVED)
                    );

            // Subquery to check organization domains
            Subquery<Long> orgDomainSubquery = query.subquery(Long.class);
            Root<UserOrganisationDomainMapping> orgMappingRoot = orgDomainSubquery.from(UserOrganisationDomainMapping.class);
            Join<UserOrganisationDomainMapping, UserDomain> orgDomainJoin = orgMappingRoot.join("domain", JoinType.INNER);

            orgDomainSubquery.select(criteriaBuilder.count(orgMappingRoot.get("id")))
                    .where(
                            criteriaBuilder.equal(orgMappingRoot.get("userUuid"), root.get("uuid")),
                            criteriaBuilder.equal(
                                    criteriaBuilder.lower(orgDomainJoin.get("domainName")),
                                    domainName.toLowerCase()
                            ),
                            criteriaBuilder.isTrue(orgMappingRoot.get("active")),
                            criteriaBuilder.isFalse(orgMappingRoot.get("deleted"))
                    );

            // User has domain if they have it either standalone OR in an organization
            return criteriaBuilder.or(
                    criteriaBuilder.greaterThan(standaloneDomainSubquery, 0L),
                    criteriaBuilder.greaterThan(orgDomainSubquery, 0L)
            );
        };
    }

    /**
     * Creates a specification to filter users belonging to a specific organization.
     *
     * @param organisationUuid The organization UUID
     * @param activeOnly Optional parameter to filter only active memberships ("true"/"false")
     * @return Specification that checks organization membership
     */
    public Specification<User> belongsToOrganisation(UUID organisationUuid, String activeOnly) {
        return (root, query, criteriaBuilder) -> {
            log.debug("Building belongsToOrganisation predicate for org: {}, activeOnly: {}", organisationUuid, activeOnly);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserOrganisationDomainMapping> mappingRoot = subquery.from(UserOrganisationDomainMapping.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(mappingRoot.get("userUuid"), root.get("uuid")));
            predicates.add(criteriaBuilder.equal(mappingRoot.get("organisationUuid"), organisationUuid));
            predicates.add(criteriaBuilder.isFalse(mappingRoot.get("deleted")));

            // If activeOnly is specified, filter by active status
            if (activeOnly != null && !activeOnly.isEmpty()) {
                boolean isActive = Boolean.parseBoolean(activeOnly);
                if (isActive) {
                    predicates.add(criteriaBuilder.isTrue(mappingRoot.get("active")));
                } else {
                    predicates.add(criteriaBuilder.isFalse(mappingRoot.get("active")));
                }
            }

            subquery.select(criteriaBuilder.count(mappingRoot.get("id")))
                    .where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

            return criteriaBuilder.greaterThan(subquery, 0L);
        };
    }

    /**
     * Creates a specification to filter users with a specific domain in a specific organization.
     *
     * @param organisationUuid The organization UUID
     * @param domainName The domain name within the organization
     * @return Specification that checks domain within organization
     */
    public Specification<User> hasDomainInOrganisation(UUID organisationUuid, String domainName) {
        return (root, query, criteriaBuilder) -> {
            log.debug("Building hasDomainInOrganisation predicate for org: {}, domain: {}", organisationUuid, domainName);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserOrganisationDomainMapping> mappingRoot = subquery.from(UserOrganisationDomainMapping.class);
            Join<UserOrganisationDomainMapping, UserDomain> domainJoin = mappingRoot.join("domain", JoinType.INNER);

            subquery.select(criteriaBuilder.count(mappingRoot.get("id")))
                    .where(
                            criteriaBuilder.equal(mappingRoot.get("userUuid"), root.get("uuid")),
                            criteriaBuilder.equal(mappingRoot.get("organisationUuid"), organisationUuid),
                            criteriaBuilder.equal(
                                    criteriaBuilder.lower(domainJoin.get("domainName")),
                                    domainName.toLowerCase()
                            ),
                            criteriaBuilder.isTrue(mappingRoot.get("active")),
                            criteriaBuilder.isFalse(mappingRoot.get("deleted"))
                    );

            return criteriaBuilder.greaterThan(subquery, 0L);
        };
    }

    /**
     * Creates a specification to filter users assigned to a specific branch.
     *
     * @param branchUuid The branch UUID
     * @return Specification that checks branch assignment
     */
    public Specification<User> belongsToBranch(UUID branchUuid) {
        return (root, query, criteriaBuilder) -> {
            log.debug("Building belongsToBranch predicate for branch: {}", branchUuid);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<UserOrganisationDomainMapping> mappingRoot = subquery.from(UserOrganisationDomainMapping.class);

            subquery.select(criteriaBuilder.count(mappingRoot.get("id")))
                    .where(
                            criteriaBuilder.equal(mappingRoot.get("userUuid"), root.get("uuid")),
                            criteriaBuilder.equal(mappingRoot.get("branchUuid"), branchUuid),
                            criteriaBuilder.isTrue(mappingRoot.get("active")),
                            criteriaBuilder.isFalse(mappingRoot.get("deleted"))
                    );

            return criteriaBuilder.greaterThan(subquery, 0L);
        };
    }
}
