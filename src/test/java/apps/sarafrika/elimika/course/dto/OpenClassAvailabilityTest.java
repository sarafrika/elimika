package apps.sarafrika.elimika.course.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Open class availability band")
class OpenClassAvailabilityTest {

    @Test
    @DisplayName("no seat left is FULL")
    void noSeatLeftIsFull() {
        assertThat(OpenClassAvailability.of(10, 0)).isEqualTo(OpenClassAvailability.FULL);
    }

    @Test
    @DisplayName("FEW_LEFT is at most five seats on a small class")
    void smallClassUsesTheFloorOfFive() {
        assertThat(OpenClassAvailability.of(10, 5)).isEqualTo(OpenClassAvailability.FEW_LEFT);
        assertThat(OpenClassAvailability.of(10, 6)).isEqualTo(OpenClassAvailability.OPEN);
    }

    @Test
    @DisplayName("FEW_LEFT is at most 20% of capacity on a large class")
    void largeClassUsesTwentyPercent() {
        assertThat(OpenClassAvailability.of(50, 10)).isEqualTo(OpenClassAvailability.FEW_LEFT);
        assertThat(OpenClassAvailability.of(50, 11)).isEqualTo(OpenClassAvailability.OPEN);
    }

    @Test
    @DisplayName("unknown capacity is OPEN")
    void unknownCapacityIsOpen() {
        assertThat(OpenClassAvailability.of(null, null)).isEqualTo(OpenClassAvailability.OPEN);
    }
}
