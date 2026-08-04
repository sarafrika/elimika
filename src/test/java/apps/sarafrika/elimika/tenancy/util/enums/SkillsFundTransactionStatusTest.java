package apps.sarafrika.elimika.tenancy.util.enums;

import apps.sarafrika.elimika.tenancy.util.converter.SkillsFundTransactionStatusConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every legacy status the code ever referenced, and where it lands.
 * <p>
 * The set is not guesswork: {@code SkillsFundServiceImpl.getSummary} filtered on
 * Completed/Disbursed/Allocated/Approved/Pending, the column default was {@code 'Pending'}, and the
 * organisation dashboard's status select offered Pending/Allocated/Approved/Completed. Those are the
 * values that can exist in the wild, and each is asserted here so the Flyway mapping and the runtime
 * converter can never disagree about one of them.
 */
@DisplayName("Skills fund transaction status: legacy value mapping")
class SkillsFundTransactionStatusTest {

    private final SkillsFundTransactionStatusConverter converter = new SkillsFundTransactionStatusConverter();

    @ParameterizedTest(name = "\"{0}\" maps to {1}")
    @CsvSource({
            "Pending,   PENDING",
            "Allocated, ALLOCATED",
            "Approved,  APPROVED",
            "Completed, DISBURSED",
            "Disbursed, DISBURSED"
    })
    @DisplayName("maps every status the legacy code referenced")
    void mapsEveryLegacyValue(String legacy, SkillsFundTransactionStatus expected) {
        assertThat(SkillsFundTransactionStatus.fromValue(legacy)).isEqualTo(expected);
        assertThat(converter.convertToEntityAttribute(legacy)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" still reads as PENDING")
    @ValueSource(strings = {"PENDING", "pending", "  Pending  ", "pEnDiNg"})
    @DisplayName("tolerates the case and whitespace of hand-edited data")
    void isCaseAndWhitespaceInsensitive(String stored) {
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(SkillsFundTransactionStatus.PENDING);
    }

    @Test
    @DisplayName("'Completed' and 'Disbursed' were always the same fact and are now the same state")
    void completedAndDisbursedCollapse() {
        assertThat(SkillsFundTransactionStatus.fromValue("Completed"))
                .isEqualTo(SkillsFundTransactionStatus.fromValue("Disbursed"))
                .isEqualTo(SkillsFundTransactionStatus.DISBURSED);
    }

    @Test
    @DisplayName("there are exactly four states — a fifth would need a migration, not a new string")
    void theValueSetIsClosed() {
        assertThat(SkillsFundTransactionStatus.values()).containsExactly(
                SkillsFundTransactionStatus.PENDING,
                SkillsFundTransactionStatus.ALLOCATED,
                SkillsFundTransactionStatus.APPROVED,
                SkillsFundTransactionStatus.DISBURSED);
    }

    @Test
    @DisplayName("a typo is rejected loudly rather than silently falling out of every total")
    void unknownValuesAreRejected() {
        assertThatThrownBy(() -> SkillsFundTransactionStatus.fromValue("Compleeted"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Compleeted");
    }

    @Test
    @DisplayName("committed means allocated, approved or already out of the door")
    void committedCoversEverythingPastPending() {
        assertThat(SkillsFundTransactionStatus.PENDING.isCommitted()).isFalse();
        assertThat(SkillsFundTransactionStatus.ALLOCATED.isCommitted()).isTrue();
        assertThat(SkillsFundTransactionStatus.APPROVED.isCommitted()).isTrue();
        assertThat(SkillsFundTransactionStatus.DISBURSED.isCommitted()).isTrue();
    }

    @Test
    @DisplayName("only DISBURSED means the money actually left")
    void onlyDisbursedIsDisbursed() {
        assertThat(SkillsFundTransactionStatus.DISBURSED.isDisbursed()).isTrue();
        assertThat(SkillsFundTransactionStatus.APPROVED.isDisbursed()).isFalse();
    }

    @Test
    @DisplayName("null and blank round-trip as null rather than a fabricated default")
    void nullAndBlankStayNull() {
        assertThat(SkillsFundTransactionStatus.fromValue(null)).isNull();
        assertThat(SkillsFundTransactionStatus.fromValue("   ")).isNull();
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("what the converter writes is exactly what the CHECK constraint allows")
    void storedFormMatchesTheEnumNames() {
        for (SkillsFundTransactionStatus status : SkillsFundTransactionStatus.values()) {
            assertThat(converter.convertToDatabaseColumn(status)).isEqualTo(status.name());
        }
    }
}
