package apps.sarafrika.elimika.classes.util.converter;

import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassServiceTypeConverterTest {

    private final ClassServiceTypeConverter converter = new ClassServiceTypeConverter();

    @Test
    void convertsEnumToColumnName() {
        assertThat(converter.convertToDatabaseColumn(ClassServiceType.PRIVATE_ONLINE)).isEqualTo("PRIVATE_ONLINE");
    }

    @Test
    void convertsNullEnumToNullColumn() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertsColumnNameBackToEnumCaseInsensitively() {
        assertThat(converter.convertToEntityAttribute("group")).isEqualTo(ClassServiceType.GROUP);
        assertThat(converter.convertToEntityAttribute(" One_On_One ")).isEqualTo(ClassServiceType.ONE_ON_ONE);
    }

    @Test
    void convertsNullOrBlankColumnToNullEnum() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("  ")).isNull();
    }
}
