package apps.sarafrika.elimika.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A generic wrapper class that provides a standardized structure for all API responses.
 * This wrapper ensures consistency across the application's API layer by including
 * success/failure status, data payload, informational messages, and error details.
 *
 * @param <T> The type of data being returned in the response
 */
@Schema(description = "Standard response wrapper for all API endpoints")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @Schema(description = "Indicates whether the operation was successful", example = "true")
        boolean success,

        @Schema(description = "The payload returned by the API (null in case of errors)")
        T data,

        @Schema(description = "A message providing additional context about the response",
                example = "User retrieved successfully")
        String message,

        @Schema(description = "Error details in case of failure (null in case of success)",
                oneOf = {String.class, Map.class})
        Object error
) {
    /**
     * Creates a success response with the provided data and message.
     *
     * @param data    The payload to include in the response
     * @param message A descriptive message about the successful operation
     * @param <T>     The type of data being returned
     * @return A new ApiResponse instance indicating success
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * Creates an error response with the provided message and error details.
     *
     * @param message      A descriptive message about the error
     * @param errorDetails Additional details about the error (can be a String, Map, or other object)
     * @param <T>          The type parameter for the response
     * @return A new ApiResponse instance indicating failure
     */
    public static <T> ApiResponse<T> error(String message, Object errorDetails) {
        return new ApiResponse<>(false, null, message, errorDetails);
    }
}