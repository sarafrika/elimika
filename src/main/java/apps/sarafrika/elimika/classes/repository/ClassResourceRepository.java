package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassResourceRepository extends JpaRepository<ClassResource, Long> {

    Optional<ClassResource> findByUuid(UUID uuid);

    List<ClassResource> findByClassDefinitionUuid(UUID classDefinitionUuid);

    List<ClassResource> findByResourceType(String resourceType);

    @Query("SELECT cr FROM ClassResource cr WHERE cr.classDefinitionUuid = :classDefinitionUuid AND cr.isActive = true")
    List<ClassResource> findActiveResourcesForClass(@Param("classDefinitionUuid") UUID classDefinitionUuid);

    @Query("SELECT cr FROM ClassResource cr WHERE cr.classDefinitionUuid = :classDefinitionUuid AND cr.resourceType = :resourceType AND cr.isActive = true")
    List<ClassResource> findActiveResourcesOfTypeForClass(@Param("classDefinitionUuid") UUID classDefinitionUuid, 
                                                          @Param("resourceType") String resourceType);
}