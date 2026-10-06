package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.internal.model.UserOwnedEntity;
import apps.sarafrika.elimika.profile.internal.model.VerifiableItem;
import apps.sarafrika.elimika.profile.internal.repository.UserOwnedRepository;
import apps.sarafrika.elimika.profile.spi.ProfessionalProfileChangedEvent;
import apps.sarafrika.elimika.profile.spi.ProfileRecords;
import apps.sarafrika.elimika.profile.spi.ProfileSection;
import apps.sarafrika.elimika.profile.spi.ProfileSectionService;
import apps.sarafrika.elimika.profile.spi.WalletVerificationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped CRUD, search and verification for one section; identical claims merge, changed claims re-verify. */
@Transactional
public abstract class ProfileSectionSupport<E extends UserOwnedEntity, D> implements ProfileSectionService<D> {

    private final Class<E> entityClass;
    private final ProfileSection section;
    protected final UserOwnedRepository<E> repository;
    private final GenericSpecificationBuilder<E> specificationBuilder;
    private final ApplicationEventPublisher eventPublisher;

    protected ProfileSectionSupport(Class<E> entityClass, ProfileSection section, UserOwnedRepository<E> repository,
                                    GenericSpecificationBuilder<E> specificationBuilder,
                                    ApplicationEventPublisher eventPublisher) {
        this.entityClass = entityClass;
        this.section = section;
        this.repository = repository;
        this.specificationBuilder = specificationBuilder;
        this.eventPublisher = eventPublisher;
    }

    protected abstract E newEntity();

    protected abstract D toDto(E entity);

    /** Copies the owner-editable fields onto the entity; true when a verified claim changed. */
    protected abstract boolean apply(E entity, D dto);

    /** Normalised identity of a claim; an identical claim updates the existing item instead of adding one. */
    protected abstract String naturalKey(D dto);

    protected abstract String naturalKeyOf(E entity);

    /** Fills what only a new item carries, such as a document's stored file. */
    protected void prepareNew(E entity, D dto) {
    }

    protected void afterDelete(E entity) {
    }

    public ProfileSection section() {
        return section;
    }

    @Override
    @Transactional(readOnly = true)
    public List<D> list(UUID userUuid) {
        if (userUuid == null) {
            return List.of();
        }
        return repository.findByUserUuidOrderByIdAsc(userUuid).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<D> listForUsers(Collection<UUID> userUuids) {
        if (userUuids == null || userUuids.isEmpty()) {
            return List.of();
        }
        return repository.findByUserUuidInOrderByIdAsc(userUuids).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<D> find(UUID itemUuid) {
        return itemUuid == null ? Optional.empty() : repository.findByUuid(itemUuid).map(this::toDto);
    }

    @Override
    public D create(UUID userUuid, D item) {
        Objects.requireNonNull(userUuid, "userUuid");
        String key = naturalKey(item);
        if (key != null) {
            Optional<E> identical = repository.findByUserUuidOrderByIdAsc(userUuid).stream()
                    .filter(existing -> key.equals(naturalKeyOf(existing)))
                    .findFirst();
            if (identical.isPresent()) {
                // Re-adding a claim fills in what it leaves blank from the existing item.
                return save(identical.get(), fillFrom(item, identical.get()));
            }
        }
        E entity = newEntity();
        entity.setUserUuid(userUuid);
        prepareNew(entity, item);
        apply(entity, item);
        E saved = repository.save(entity);
        publish(userUuid);
        return toDto(saved);
    }

    @Override
    public D update(UUID userUuid, UUID itemUuid, D item) {
        return save(owned(userUuid, itemUuid), item);
    }

    @Override
    public void delete(UUID userUuid, UUID itemUuid) {
        E entity = owned(userUuid, itemUuid);
        repository.delete(entity);
        afterDelete(entity);
        publish(userUuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<D> search(Map<String, String> searchParams, Collection<UUID> restrictToUsers, Pageable pageable) {
        specificationBuilder.validateSortProperties(entityClass, pageable);
        if (restrictToUsers != null && restrictToUsers.isEmpty()) {
            return Page.empty(pageable);
        }
        Specification<E> spec = specificationBuilder.buildSpecification(entityClass, searchParams);
        if (restrictToUsers != null) {
            Specification<E> owners = (root, query, cb) -> root.get("userUuid").in(restrictToUsers);
            spec = spec == null ? owners : spec.and(owners);
        }
        return (spec == null ? repository.findAll(pageable) : repository.findAll(spec, pageable)).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(UUID userUuid) {
        return userUuid == null ? 0 : repository.countByUserUuid(userUuid);
    }

    /** Records an admin verdict on an owned item. */
    public void verify(UUID userUuid, UUID itemUuid, WalletVerificationStatus status, String notes, String verifiedBy) {
        E entity = owned(userUuid, itemUuid);
        recordVerdict(entity, status, notes, verifiedBy, LocalDateTime.now(ZoneOffset.UTC));
        repository.save(entity);
        publish(userUuid);
    }

    protected void recordVerdict(E entity, WalletVerificationStatus status, String notes, String verifiedBy,
                                 LocalDateTime at) {
        if (!(entity instanceof VerifiableItem verifiable)) {
            throw new IllegalArgumentException("Items in " + section.path() + " are not verified");
        }
        verifiable.recordVerdict(status, notes, at);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> ownerOf(UUID itemUuid) {
        return itemUuid == null ? Optional.empty() : repository.findByUuid(itemUuid).map(UserOwnedEntity::getUserUuid);
    }

    protected E owned(UUID userUuid, UUID itemUuid) {
        return repository.findByUuid(itemUuid)
                .filter(found -> userUuid != null && userUuid.equals(found.getUserUuid()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Profile %s item with ID %s not found", section.path(), itemUuid)));
    }

    private D save(E entity, D item) {
        boolean claimChanged = apply(entity, item);
        if (claimChanged && entity instanceof VerifiableItem verifiable) {
            verifiable.resetVerification();
        }
        E saved = repository.save(entity);
        publish(saved.getUserUuid());
        return toDto(saved);
    }

    @SuppressWarnings("unchecked")
    private D fillFrom(D item, E existing) {
        D current = toDto(existing);
        if (item instanceof Record changes && current instanceof Record base) {
            return (D) ProfileRecords.overlay(changes, base);
        }
        return item;
    }

    private void publish(UUID userUuid) {
        eventPublisher.publishEvent(new ProfessionalProfileChangedEvent(userUuid, section, null));
    }

    /** Lower-cased, trimmed, inner whitespace collapsed; null and blank compare equal. */
    protected static String norm(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    protected static String key(Object... parts) {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            builder.append(norm(part)).append('|');
        }
        return builder.toString();
    }

    protected static boolean changed(Object before, Object after) {
        return !norm(before).equals(norm(after));
    }
}
