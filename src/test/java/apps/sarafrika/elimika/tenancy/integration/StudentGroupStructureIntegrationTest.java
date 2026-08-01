package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.tenancy.dto.StudentGroupRosterEntryDTO;
import apps.sarafrika.elimika.tenancy.entity.AcademicTier;
import apps.sarafrika.elimika.tenancy.repository.AcademicTierRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the structured student group schema against a real PostgreSQL instance.
 * <p>
 * Three things here cannot be shown with mocked repositories: that the academic tier seed actually
 * lands as sixteen ordered rows, that the roster projection is valid HQL that PostgreSQL will run,
 * and that the new constraints on {@code student_groups} behave as written. The last two in
 * particular are the kind of thing that only fails once the migrations and the entity model are
 * put in the same room.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@DisplayName("Academic tier seed, roster projection and student group constraints")
class StudentGroupStructureIntegrationTest {

    private static final List<String> KENYAN_TIERS = List.of(
            "Kindergarten", "PP1", "PP2",
            "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6",
            "Grade 7", "Grade 8", "Grade 9",
            "Form 1", "Form 2", "Form 3", "Form 4");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        // The real migrations build the schema; Hibernate must not touch it. Full `validate` is not
        // usable here because the test source set contributes its own throwaway @Entity classes
        // that have no table — the mapping is still exercised, since every query below runs against
        // the migrated schema and fails loudly on any column that is not there.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private AcademicTierRepository academicTierRepository;
    @Autowired
    private StudentGroupMemberRepository studentGroupMemberRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID organisationUuid;
    private UUID westlandsBranchUuid;
    private UUID karenBranchUuid;
    private UUID grade9Uuid;
    private UUID form1Uuid;

    @BeforeEach
    void seedOrganisation() {
        organisationUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')",
                organisationUuid, "Test School");

        westlandsBranchUuid = insertBranch("Westlands Campus");
        karenBranchUuid = insertBranch("Karen Campus");

        grade9Uuid = tierUuid("Grade 9");
        form1Uuid = tierUuid("Form 1");
    }

    @Test
    @DisplayName("the seed produces exactly sixteen Kenyan tiers, ordered and gapped by tens")
    void seedProducesSixteenOrderedTiers() {
        List<AcademicTier> tiers = academicTierRepository
                .findByEducationSystemIgnoreCaseAndOrganisationUuidIsNullAndActiveTrueOrderByTierOrderAsc("KE");

        assertThat(tiers).hasSize(16);
        assertThat(tiers).extracting(AcademicTier::getName).containsExactlyElementsOf(KENYAN_TIERS);
        assertThat(tiers).extracting(AcademicTier::getTierOrder)
                .containsExactly(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160);
        assertThat(tiers).allSatisfy(tier -> {
            assertThat(tier.getEducationSystem()).isEqualTo("KE");
            assertThat(tier.getOrganisationUuid()).isNull();
            assertThat(tier.isActive()).isTrue();
        });
    }

    @Test
    @DisplayName("the roster projection joins members, users, groups and tiers into one paginated table")
    void rosterProjectionReturnsAFullRow() {
        UUID groupUuid = insertGroup("Grade 9 Blue", "Blue", westlandsBranchUuid, grade9Uuid, 40);
        UUID studentUuid = insertUser("Asha", "Kimani", LocalDate.of(2011, 3, 4));
        insertMember(groupUuid, studentUuid);

        Page<StudentGroupRosterEntryDTO> page = studentGroupMemberRepository
                .findRoster(organisationUuid, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        StudentGroupRosterEntryDTO entry = page.getContent().getFirst();
        assertThat(entry.studentUuid()).isEqualTo(studentUuid);
        assertThat(entry.groupUuid()).isEqualTo(groupUuid);
        assertThat(entry.groupName()).isEqualTo("Grade 9 Blue");
        assertThat(entry.tier()).isEqualTo("Grade 9");
        assertThat(entry.streamLabel()).isEqualTo("Blue");
        assertThat(entry.fullName()).isEqualTo("Asha Kimani");
        assertThat(entry.email()).isNotBlank();
        assertThat(entry.dob()).isEqualTo(LocalDate.of(2011, 3, 4));
        assertThat(entry.joinedDate()).isNotNull();
    }

    @Test
    @DisplayName("the roster filters by branch, tier and group, and pages in the database")
    void rosterFiltersAndPages() {
        UUID westlandsGrade9 = insertGroup("Grade 9 Blue", "Blue", westlandsBranchUuid, grade9Uuid, null);
        UUID westlandsForm1 = insertGroup("Form 1 Blue", "Blue", westlandsBranchUuid, form1Uuid, null);
        UUID karenGrade9 = insertGroup("Grade 9 Red", "Red", karenBranchUuid, grade9Uuid, null);
        UUID legacy = insertGroup("Old cohort", null, null, null, null);

        insertMember(westlandsGrade9, insertUser("A", "One", null));
        insertMember(westlandsGrade9, insertUser("B", "Two", null));
        insertMember(westlandsForm1, insertUser("C", "Three", null));
        insertMember(karenGrade9, insertUser("D", "Four", null));
        insertMember(legacy, insertUser("E", "Five", null));

        assertThat(roster(null, null, null).getTotalElements()).isEqualTo(5);
        assertThat(roster(westlandsBranchUuid, null, null).getTotalElements()).isEqualTo(3);
        assertThat(roster(null, grade9Uuid, null).getTotalElements()).isEqualTo(3);
        assertThat(roster(westlandsBranchUuid, grade9Uuid, null).getTotalElements()).isEqualTo(2);
        assertThat(roster(null, null, legacy).getTotalElements()).isEqualTo(1);

        Page<StudentGroupRosterEntryDTO> firstPage = studentGroupMemberRepository
                .findRoster(organisationUuid, null, null, null, PageRequest.of(0, 2));
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("an unassigned legacy group still appears on the roster with a null tier")
    void rosterKeepsUnassignedGroups() {
        UUID legacy = insertGroup("Old cohort", null, null, null, null);
        insertMember(legacy, insertUser("Legacy", "Learner", null));

        StudentGroupRosterEntryDTO entry = roster(null, null, null).getContent().getFirst();

        assertThat(entry.tier()).isNull();
        assertThat(entry.streamLabel()).isNull();
        assertThat(entry.groupName()).isEqualTo("Old cohort");
    }

    @Test
    @DisplayName("the roster never crosses organisations")
    void rosterIsScopedToOneOrganisation() {
        UUID otherOrganisation = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')",
                otherOrganisation, "Rival School");
        UUID rivalGroup = UUID.randomUUID();
        jdbc.update("INSERT INTO student_groups (uuid, organisation_uuid, name) VALUES (?, ?, ?)",
                rivalGroup, otherOrganisation, "Rival cohort");
        insertMember(rivalGroup, insertUser("Rival", "Student", null));

        assertThat(roster(null, null, null).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("one stream label may exist only once per branch and tier")
    void structuredGroupsAreUniquePerBranchAndTier() {
        insertGroup("Grade 9 Blue", "Blue", westlandsBranchUuid, grade9Uuid, null);
        // A different branch, or a different tier, is a different group entirely. Asserted before
        // the collision because a constraint violation aborts the surrounding transaction.
        insertGroup("Grade 9 Blue at Karen", "Blue", karenBranchUuid, grade9Uuid, null);
        insertGroup("Form 1 Blue", "Blue", westlandsBranchUuid, form1Uuid, null);

        // The index is on lower(group_type), so a difference in case is not a difference.
        assertThatThrownBy(() -> insertGroup("Grade 9 Blue again", "blue", westlandsBranchUuid, grade9Uuid, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("legacy groups with no branch or tier are exempt from the structured uniqueness rule")
    void unassignedGroupsDoNotCollide() {
        insertGroup("Old cohort", "Blue", null, null, null);
        insertGroup("Another old cohort", "Blue", null, null, null);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student_groups WHERE organisation_uuid = ? AND branch_uuid IS NULL",
                Integer.class, organisationUuid);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("capacity must be positive when supplied, and may be absent")
    void capacityIsCheckedButOptional() {
        insertGroup("No capacity", "A", westlandsBranchUuid, grade9Uuid, null);

        assertThatThrownBy(() -> insertGroup("Zero capacity", "B", westlandsBranchUuid, grade9Uuid, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Page<StudentGroupRosterEntryDTO> roster(UUID branchUuid, UUID tierUuid, UUID groupUuid) {
        return studentGroupMemberRepository
                .findRoster(organisationUuid, branchUuid, tierUuid, groupUuid, PageRequest.of(0, 50));
    }

    private UUID tierUuid(String name) {
        return UUID.fromString(jdbc.queryForObject(
                "SELECT uuid::text FROM academic_tiers WHERE name = ? AND organisation_uuid IS NULL",
                String.class, name));
    }

    private UUID insertBranch(String branchName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_branches (uuid, organisation_uuid, branch_name, created_by) "
                + "VALUES (?, ?, ?, 'test')", uuid, organisationUuid, branchName);
        return uuid;
    }

    private UUID insertGroup(String name, String groupType, UUID branchUuid, UUID tierUuid, Integer capacity) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO student_groups (uuid, organisation_uuid, name, group_type, branch_uuid, tier_uuid, capacity) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                uuid, organisationUuid, name, groupType, branchUuid, tierUuid, capacity);
        return uuid;
    }

    private UUID insertUser(String firstName, String lastName, LocalDate dob) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, dob, user_no, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, lpad(nextval('user_no_seq')::text, 9, '0'), 'test')",
                uuid, firstName, lastName, "u" + Long.toHexString(System.nanoTime()) + "@example.test", dob);
        return uuid;
    }

    private void insertMember(UUID groupUuid, UUID studentUuid) {
        jdbc.update("INSERT INTO student_group_members (uuid, group_uuid, student_uuid) VALUES (?, ?, ?)",
                UUID.randomUUID(), groupUuid, studentUuid);
    }
}
