package apps.sarafrika.elimika.shared.config;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The translation from a database constraint failure into something a user can act on.
 * <p>
 * This exists because the not-null branch silently never fired. PostgreSQL writes
 * "violates not-null constraint" with a hyphen, the handler tested for the spaced "not null", and so
 * every missing-column failure fell through to the catch-all string. On staging that turned 140
 * consecutive create-class and add-assessment failures into "The operation cannot be completed due
 * to a data constraint violation" — true, and of no use to anyone diagnosing it.
 */
@DisplayName("Data integrity violations translated for the client")
class GlobalExceptionHandlerDataIntegrityTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', value = {
            "ERROR: null value in column \"rate_basis\" of relation \"class_definitions\" violates not-null constraint | Required information is missing",
            "ERROR: null value in column \"active\" of relation \"course_assessments\" violates not-null constraint    | Required information is missing",
            "ERROR: insert or update on table \"x\" violates foreign key constraint \"fk_x\"                            | Cannot complete operation: referenced record does not exist or is in use",
            "ERROR: duplicate key value violates unique constraint \"uk_x\"                                            | A record with this information already exists",
            "ERROR: new row for relation \"x\" violates check constraint \"chk_x\"                                      | The provided data does not meet validation requirements",
    })
    @DisplayName("each constraint kind gets its own message")
    void translatesEachConstraintKind(String driverMessage, String expectedMessage) {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleDataIntegrityViolationException(new DataIntegrityViolationException(driverMessage));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("an unrecognised violation still falls back to the generic message")
    void unrecognisedViolationFallsBack() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(
                new DataIntegrityViolationException("ERROR: something the driver has never said before"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("The operation cannot be completed due to a data constraint violation");
    }
}
