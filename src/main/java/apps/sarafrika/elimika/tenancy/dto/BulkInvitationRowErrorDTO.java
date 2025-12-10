package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single row error encountered during bulk invitation processing.
 */
public record BulkInvitationRowErrorDTO(

        @JsonProperty("row_number")
        int rowNumber,

        @JsonProperty("recipient_email")
        String recipientEmail,

        @JsonProperty("error_message")
        String errorMessage
) {
}
