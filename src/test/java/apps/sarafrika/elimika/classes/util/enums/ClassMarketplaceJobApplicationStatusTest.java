package apps.sarafrika.elimika.classes.util.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the recruitment funnel's order, which this enum owns:
 * applied - shortlisted - interviewing - offered - hired, then assigned when the class is created.
 */
class ClassMarketplaceJobApplicationStatusTest {

    private static final List<ClassMarketplaceJobApplicationStatus> FUNNEL = List.of(
            ClassMarketplaceJobApplicationStatus.PENDING,
            ClassMarketplaceJobApplicationStatus.SHORTLISTED,
            ClassMarketplaceJobApplicationStatus.INTERVIEWING,
            ClassMarketplaceJobApplicationStatus.OFFERED,
            ClassMarketplaceJobApplicationStatus.HIRED,
            ClassMarketplaceJobApplicationStatus.ASSIGNED);

    private static final List<ClassMarketplaceJobApplicationStatus> LIVE_STAGES = List.of(
            ClassMarketplaceJobApplicationStatus.PENDING,
            ClassMarketplaceJobApplicationStatus.SHORTLISTED,
            ClassMarketplaceJobApplicationStatus.INTERVIEWING,
            ClassMarketplaceJobApplicationStatus.OFFERED,
            ClassMarketplaceJobApplicationStatus.HIRED);

    private static final List<ClassMarketplaceJobApplicationStatus> EXITS = List.of(
            ClassMarketplaceJobApplicationStatus.REJECTED,
            ClassMarketplaceJobApplicationStatus.NOT_SELECTED,
            ClassMarketplaceJobApplicationStatus.WITHDRAWN);

    @Test
    void everyLegalTransitionIsAcceptedAndEverySkippedOneRefused() {
        for (int from = 0; from < FUNNEL.size(); from++) {
            for (int to = 0; to < FUNNEL.size(); to++) {
                boolean oneStepForward = to == from + 1;
                assertThat(FUNNEL.get(to).isReachableFrom(FUNNEL.get(from)))
                        .as("%s -> %s", FUNNEL.get(from), FUNNEL.get(to))
                        .isEqualTo(oneStepForward);
            }
        }
    }

    @Test
    void everyStageNamesTheOneDecisionThatPrecedesIt() {
        for (int stage = 1; stage < FUNNEL.size(); stage++) {
            assertThat(FUNNEL.get(stage).previousStage()).isEqualTo(FUNNEL.get(stage - 1));
        }
        assertThat(ClassMarketplaceJobApplicationStatus.PENDING.previousStage()).isNull();
    }

    @Test
    void refusingAndWithdrawingAreReachableFromEveryLiveStage() {
        for (ClassMarketplaceJobApplicationStatus exit : EXITS) {
            assertThat(exit.isExit()).isTrue();
            for (ClassMarketplaceJobApplicationStatus stage : LIVE_STAGES) {
                assertThat(exit.isReachableFrom(stage)).as("%s -> %s", stage, exit).isTrue();
            }
        }
    }

    @Test
    void nothingLeavesAClosedApplication() {
        List<ClassMarketplaceJobApplicationStatus> closed = List.of(
                ClassMarketplaceJobApplicationStatus.ASSIGNED,
                ClassMarketplaceJobApplicationStatus.REJECTED,
                ClassMarketplaceJobApplicationStatus.NOT_SELECTED,
                ClassMarketplaceJobApplicationStatus.WITHDRAWN);

        for (ClassMarketplaceJobApplicationStatus from : closed) {
            for (ClassMarketplaceJobApplicationStatus to : ClassMarketplaceJobApplicationStatus.values()) {
                assertThat(to.isReachableFrom(from)).as("%s -> %s", from, to).isFalse();
            }
        }
        assertThat(ClassMarketplaceJobApplicationStatus.HIRED.isReachableFrom(null)).isFalse();
    }
}
