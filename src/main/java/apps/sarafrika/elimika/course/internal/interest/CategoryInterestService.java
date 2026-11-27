package apps.sarafrika.elimika.course.internal.interest;

import apps.sarafrika.elimika.course.dto.CategoryInterestsDTO;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.CategoryInterest;
import apps.sarafrika.elimika.course.repository.CategoryInterestRepository;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The course categories the caller picked as interests, one set per domain. The domain need not be held yet:
 * onboarding asks for interests before the domain is requested.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryInterestService {

    static final Set<UserDomain> DOMAINS = Set.of(
            UserDomain.student, UserDomain.instructor, UserDomain.course_creator, UserDomain.organisation_user);

    private final CategoryInterestRepository repository;
    private final CategoryRepository categoryRepository;
    private final DomainSecurityService domainSecurityService;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public CategoryInterestsDTO mine(String domain) {
        return toDTO(parse(domain), currentUser());
    }

    /** Replaces the caller's interests for the domain; 400 for a null entry or an unknown or inactive category. */
    public CategoryInterestsDTO replaceMine(String domain, List<UUID> categoryUuids) {
        UserDomain userDomain = parse(domain);
        UUID userUuid = currentUser();
        if (categoryUuids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("category_uuids must not contain null");
        }
        List<UUID> wanted = categoryUuids.stream().distinct().toList();

        Set<UUID> active = categoryRepository.findByUuidIn(wanted).stream()
                .filter(category -> !Boolean.FALSE.equals(category.getIsActive()))
                .map(Category::getUuid).collect(Collectors.toSet());
        List<UUID> refused = wanted.stream().filter(uuid -> !active.contains(uuid)).toList();
        if (!refused.isEmpty()) {
            throw new IllegalArgumentException("Unknown or inactive category: " + refused);
        }

        // One replace at a time per user and domain: two overlapping saves would otherwise both reinsert and
        // collide on the unique key. The lock is released when the transaction ends.
        jdbc.queryForObject("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:key, 0))",
                Map.of("key", "category_interests:" + userUuid + ":" + userDomain), Integer.class);
        // Delete and reinsert so the saved order is the order sent.
        repository.deleteByUserUuidAndUserDomain(userUuid, userDomain);
        repository.saveAll(wanted.stream().map(categoryUuid -> {
            CategoryInterest interest = new CategoryInterest();
            interest.setUserUuid(userUuid);
            interest.setUserDomain(userDomain);
            interest.setCategoryUuid(categoryUuid);
            return interest;
        }).toList());
        return toDTO(userDomain, userUuid);
    }

    private CategoryInterestsDTO toDTO(UserDomain domain, UUID userUuid) {
        List<UUID> categoryUuids = repository.findByUserUuidAndUserDomainOrderByIdAsc(userUuid, domain).stream()
                .map(CategoryInterest::getCategoryUuid).toList();
        Map<UUID, Category> categories = categoryRepository.findByUuidIn(categoryUuids).stream()
                .collect(Collectors.toMap(Category::getUuid, Function.identity()));
        return new CategoryInterestsDTO(domain, categoryUuids.stream()
                .map(uuid -> new CategoryInterestsDTO.Category(uuid, categories.get(uuid).getName()))
                .toList());
    }

    private UUID currentUser() {
        UUID userUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid == null) {
            throw new ResourceNotFoundException("No user record for the authenticated caller");
        }
        return userUuid;
    }

    private static UserDomain parse(String domain) {
        UserDomain parsed;
        try {
            parsed = UserDomain.valueOf(domain.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
        if (!DOMAINS.contains(parsed)) {
            throw new IllegalArgumentException("Interests are not kept for domain: " + domain);
        }
        return parsed;
    }
}
