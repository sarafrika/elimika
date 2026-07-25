package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, Long> {

    List<StudentGroupMember> findByGroupUuid(UUID groupUuid);

    long countByGroupUuid(UUID groupUuid);

    boolean existsByGroupUuidAndStudentUuid(UUID groupUuid, UUID studentUuid);

    void deleteByGroupUuidAndStudentUuid(UUID groupUuid, UUID studentUuid);

    /** Member counts for a set of groups, as [groupUuid, count] rows. */
    @Query("SELECT m.groupUuid, COUNT(m) FROM StudentGroupMember m WHERE m.groupUuid IN :groupUuids GROUP BY m.groupUuid")
    List<Object[]> countByGroupUuids(@Param("groupUuids") List<UUID> groupUuids);
}
