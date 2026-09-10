package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassMarketplaceJobApplicationStatusConverterTest {

    private final ClassMarketplaceJobApplicationStatusConverter converter =
            new ClassMarketplaceJobApplicationStatusConverter();

    @Test
    void convertsHiredToItsColumnValue() {
        assertThat(converter.convertToDatabaseColumn(ClassMarketplaceJobApplicationStatus.HIRED))
                .isEqualTo("HIRED");
    }

    @Test
    void roundTripsHiredThroughTheColumn() {
        String column = converter.convertToDatabaseColumn(ClassMarketplaceJobApplicationStatus.HIRED);

        assertThat(converter.convertToEntityAttribute(column))
                .isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
    }

    @Test
    void readsHiredBackCaseInsensitively() {
        assertThat(converter.convertToEntityAttribute("hired"))
                .isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
        assertThat(converter.convertToEntityAttribute("Hired"))
                .isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
    }

    @Test
    void hiredIsTheWireValueTheFrontendSees() {
        assertThat(ClassMarketplaceJobApplicationStatus.HIRED.getValue()).isEqualTo("hired");
        assertThat(ClassMarketplaceJobApplicationStatus.fromValue("hired"))
                .isEqualTo(ClassMarketplaceJobApplicationStatus.HIRED);
    }

    @Test
    void everyStatusRoundTripsThroughTheColumn() {
        for (ClassMarketplaceJobApplicationStatus status : ClassMarketplaceJobApplicationStatus.values()) {
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(status)))
                    .isEqualTo(status);
        }
    }

    @Test
    void convertsNullsBothWays() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
