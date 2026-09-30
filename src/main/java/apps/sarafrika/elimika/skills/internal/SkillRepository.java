package apps.sarafrika.elimika.skills.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByUuid(UUID uuid);

    List<Skill> findByUuidInOrderByNameAsc(Collection<UUID> uuids);

    List<Skill> findBySlugInOrderByNameAsc(Collection<String> slugs);

    Optional<Skill> findBySlug(String slug);

    List<Skill> findAllByOrderByNameAsc();

    List<Skill> findByActiveTrueOrderByNameAsc();

    boolean existsByParentUuid(UUID parentUuid);
}
