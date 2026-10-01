package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.internal.BranchLocationResolver;
import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassMarketplaceJob;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobRepository;
import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobSessionTemplateRepository;
import apps.sarafrika.elimika.classes.repository.ClassSessionTemplateRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.skills.spi.SkillLookupService;
import apps.sarafrika.elimika.tenancy.spi.BranchLocation;
import apps.sarafrika.elimika.tenancy.spi.OrganisationLookupService;
import apps.sarafrika.elimika.tenancy.spi.TrainingBranchLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Indexing a batch resolves every branch it references (names and near-me pins) with one lookup.
 */
@ExtendWith(MockitoExtension.class)
class SearchSourceBranchBatchingTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID BRANCH_A = UUID.randomUUID();
    private static final UUID BRANCH_B = UUID.randomUUID();

    @Mock private TrainingBranchLookupService branchLookup;
    @Mock private ClassDefinitionRepository classDefinitionRepository;
    @Mock private ClassSessionTemplateRepository classSessionTemplateRepository;
    @Mock private ClassMarketplaceJobRepository jobRepository;
    @Mock private ClassMarketplaceJobSessionTemplateRepository jobSessionTemplateRepository;
    @Mock private CourseInfoService courseInfoService;
    @Mock private OrganisationLookupService organisationLookupService;
    @Mock private InstructorLookupService instructorLookupService;
    @Mock private JobRequiredSkills jobRequiredSkills;
    @Mock private SkillLookupService skillLookupService;

    @Test
    void classBatchMakesOneBranchQuery() {
        stubBranches();
        List<ClassDefinition> classes = new ArrayList<>();
        for (UUID branch : List.of(BRANCH_A, BRANCH_B, BRANCH_A, BRANCH_B)) {
            ClassDefinition definition = new ClassDefinition();
            definition.setUuid(UUID.randomUUID());
            definition.setOrganisationUuid(ORG);
            definition.setBranchUuid(branch);
            definition.setLocationType(LocationType.IN_PERSON);
            classes.add(definition);
        }
        when(classDefinitionRepository.findByUuidIn(anyCollection())).thenReturn(classes);
        ClassSearchSource source = new ClassSearchSource(classDefinitionRepository, classSessionTemplateRepository,
                courseInfoService, organisationLookupService, instructorLookupService,
                new BranchLocationResolver(branchLookup));

        List<ClassSearchDocument> documents = source.loadByUuids(List.of(UUID.randomUUID()));

        assertThat(documents).hasSize(4).allSatisfy(document -> assertThat(document.geo()).isNotNull());
        assertThat(documents).extracting(ClassSearchDocument::branchName)
                .containsExactly("Branch A", "Branch B", "Branch A", "Branch B");
        verifyOneBranchQuery();
    }

    @Test
    void jobBatchMakesOneBranchQuery() {
        stubBranches();
        List<ClassMarketplaceJob> jobs = new ArrayList<>();
        for (UUID branch : List.of(BRANCH_A, BRANCH_B, BRANCH_B)) {
            ClassMarketplaceJob job = new ClassMarketplaceJob();
            job.setUuid(UUID.randomUUID());
            job.setOrganisationUuid(ORG);
            job.setBranchUuid(branch);
            job.setLocationType(LocationType.HYBRID);
            jobs.add(job);
        }
        when(jobRepository.findByUuidIn(anyCollection())).thenReturn(jobs);
        MarketplaceJobSearchSource source = new MarketplaceJobSearchSource(jobRepository, jobSessionTemplateRepository,
                courseInfoService, organisationLookupService, new BranchLocationResolver(branchLookup),
                jobRequiredSkills, skillLookupService);

        List<MarketplaceJobSearchDocument> documents = source.loadByUuids(List.of(UUID.randomUUID()));

        assertThat(documents).hasSize(3).allSatisfy(document -> assertThat(document.geo()).isNotNull());
        assertThat(documents).extracting(MarketplaceJobSearchDocument::branchName)
                .containsExactly("Branch A", "Branch B", "Branch B");
        verifyOneBranchQuery();
    }

    private void stubBranches() {
        when(branchLookup.findBranches(anyCollection())).thenAnswer(invocation -> {
            Collection<UUID> requested = invocation.getArgument(0);
            assertThat(requested).containsExactlyInAnyOrder(BRANCH_A, BRANCH_B);
            return Map.of(
                    BRANCH_A, new BranchLocation(BRANCH_A, ORG, "Branch A", null,
                            new BigDecimal("-1.28"), new BigDecimal("36.82"), true),
                    BRANCH_B, new BranchLocation(BRANCH_B, ORG, "Branch B", null,
                            new BigDecimal("-4.04"), new BigDecimal("39.66"), true));
        });
    }

    private void verifyOneBranchQuery() {
        verify(branchLookup, times(1)).findBranches(anyCollection());
        verify(branchLookup, never()).findBranch(any(), any());
        verify(branchLookup, never()).findBranchNames(anyCollection());
    }
}
