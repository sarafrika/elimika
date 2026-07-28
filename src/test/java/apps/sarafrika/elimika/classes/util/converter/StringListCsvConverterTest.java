package apps.sarafrika.elimika.classes.util.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListCsvConverterTest {

    private final StringListCsvConverter converter = new StringListCsvConverter();

    @Test
    void joinsListIntoCsvColumn() {
        assertThat(converter.convertToDatabaseColumn(List.of("Grade 1", "Grade 2", "Junior Secondary")))
                .isEqualTo("Grade 1,Grade 2,Junior Secondary");
    }

    @Test
    void trimsAndDropsBlankEntriesWhenJoining() {
        assertThat(converter.convertToDatabaseColumn(List.of("  Grade 1  ", "  ", "Grade 3")))
                .isEqualTo("Grade 1,Grade 3");
    }

    @Test
    void convertsNullOrEmptyListToNullColumn() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
    }

    @Test
    void splitsCsvColumnBackIntoTrimmedList() {
        assertThat(converter.convertToEntityAttribute("Grade 1, Grade 2 ,Grade 3"))
                .containsExactly("Grade 1", "Grade 2", "Grade 3");
    }

    @Test
    void convertsNullOrBlankColumnToEmptyList() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void roundTripsThroughColumnAndBack() {
        List<String> original = List.of("Nursery", "Grade 4");
        String column = converter.convertToDatabaseColumn(original);
        assertThat(converter.convertToEntityAttribute(column)).containsExactlyElementsOf(original);
    }
}
