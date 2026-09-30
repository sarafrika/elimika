package apps.sarafrika.elimika.classes.internal.matching;

import apps.sarafrika.elimika.classes.internal.JobRequiredSkills;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.search.SearchGeoPoint;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JobMatchScoringTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 25);
    private static final UUID PYTHON = UUID.randomUUID();
    private static final UUID SQL = UUID.randomUUID();

    @Test
    void appliesTheWeightedFormula() {
        // skills 1/2 (0.5), 5-10 km (0.75), pay 25% above (0.5), 5+ years (1.0), bayes 4.0 (0.8), closes in 3 days (1.0)
        var result = JobMatchScoring.score(new JobMatchScoring.Inputs(
                List.of(tag(PYTHON, ProficiencyLevel.BEGINNER, false), tag(SQL, ProficiencyLevel.EXPERT, false)),
                Map.of(PYTHON, ProficiencyLevel.ADVANCED, SQL, ProficiencyLevel.ADVANCED),
                LocationType.IN_PERSON, "5-10 km", new BigDecimal("250"), new BigDecimal("200"),
                7, 4.0, TODAY.plusDays(3), TODAY, false));

        double expected = 0.35 * 0.5 + 0.20 * 0.75 + 0.15 * 0.5 + 0.10 * 1.0 + 0.10 * 0.8 + 0.10 * 1.0;
        assertThat(result.score()).isCloseTo(expected, within(0.0001));
        assertThat(result.matchedSkillUuids()).containsExactly(PYTHON);
        assertThat(result.requiredSkillCount()).isEqualTo(2);
        assertThat(result.payAbovePercent()).isEqualTo(25);
    }

    @Test
    void aMissingMandatorySkillCutsTheScoreToThirtyPercent() {
        var base = inputs(List.of(tag(PYTHON, ProficiencyLevel.BEGINNER, false)), Map.of(), false);
        var mandatory = inputs(List.of(tag(PYTHON, ProficiencyLevel.BEGINNER, true)), Map.of(), false);

        assertThat(JobMatchScoring.score(mandatory).missingMandatory()).isTrue();
        assertThat(JobMatchScoring.score(mandatory).score())
                .isCloseTo(JobMatchScoring.score(base).score() * 0.3, within(0.0001));
    }

    @Test
    void noRequiredSkillsIsFullCoverageWithNothingToCount() {
        var result = JobMatchScoring.score(inputs(List.of(), Map.of(), false));

        assertThat(result.skillCoverage()).isEqualTo(1.0);
        assertThat(result.requiredSkillCount()).isZero();
        assertThat(result.matchedSkillUuids()).isEmpty();
    }

    @Test
    void thePreferredInstructorGetsABonusAndTheScoreStaysWithinOne() {
        var plain = JobMatchScoring.score(inputs(List.of(), Map.of(), false));
        var preferred = JobMatchScoring.score(inputs(List.of(), Map.of(), true));

        assertThat(preferred.score()).isCloseTo(Math.min(1.0, plain.score() + 0.1), within(0.0001));
        assertThat(preferred.score()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void rateFitIsTheClippedMarginDoubled() {
        assertThat(JobMatchScoring.rateFit(new BigDecimal("200"), new BigDecimal("200"))).isZero();
        assertThat(JobMatchScoring.rateFit(new BigDecimal("240"), new BigDecimal("200"))).isCloseTo(0.4, within(1e-9));
        assertThat(JobMatchScoring.rateFit(new BigDecimal("500"), new BigDecimal("200"))).isEqualTo(1.0);
        assertThat(JobMatchScoring.rateFit(new BigDecimal("150"), new BigDecimal("200"))).isZero();
        assertThat(JobMatchScoring.rateFit(new BigDecimal("150"), null)).isZero();
    }

    @Test
    void locationIsFullForOnlineAndNeutralWithoutADistance() {
        assertThat(JobMatchScoring.locationFit(LocationType.ONLINE, null)).isEqualTo(1.0);
        assertThat(JobMatchScoring.locationFit(LocationType.IN_PERSON, null)).isEqualTo(0.5);
        assertThat(JobMatchScoring.locationFit(LocationType.IN_PERSON, "<2 km")).isEqualTo(1.0);
        assertThat(JobMatchScoring.locationFit(LocationType.IN_PERSON, ">25 km")).isEqualTo(0.2);
    }

    @Test
    void distanceIsOnlyEverABand() {
        assertThat(JobMatchScoring.distanceBand(null, new SearchGeoPoint(-1.29, 36.82))).isNull();
        assertThat(JobMatchScoring.distanceBand(new SearchGeoPoint(-1.29, 36.82), new SearchGeoPoint(-1.29, 36.83)))
                .isEqualTo("<2 km");
        assertThat(JobMatchScoring.distanceReason("5-10 km")).isEqualTo("About 5-10 km away");
        assertThat(JobMatchScoring.distanceReason("<2 km")).isEqualTo("Under 2 km away");
        assertThat(JobMatchScoring.distanceReason(">25 km")).isEqualTo("Over 25 km away");
    }

    @Test
    void urgencyRisesAsRegistrationCloses() {
        assertThat(JobMatchScoring.urgency(TODAY.plusDays(2), TODAY)).isEqualTo(1.0);
        assertThat(JobMatchScoring.urgency(TODAY.plusDays(30), TODAY)).isZero();
        assertThat(JobMatchScoring.urgency(TODAY.plusDays(16), TODAY)).isBetween(0.0, 1.0);
        assertThat(JobMatchScoring.urgency(null, TODAY)).isEqualTo(0.25);
    }

    private static JobMatchScoring.Inputs inputs(List<JobRequiredSkills.Tag> tags, Map<UUID, ProficiencyLevel> held,
                                                 boolean preferred) {
        return new JobMatchScoring.Inputs(tags, held, LocationType.ONLINE, null, new BigDecimal("300"),
                new BigDecimal("200"), 10, 5.0, TODAY.plusDays(1), TODAY, preferred);
    }

    private static JobRequiredSkills.Tag tag(UUID skill, ProficiencyLevel min, boolean mandatory) {
        return new JobRequiredSkills.Tag(skill, min, mandatory);
    }
}
