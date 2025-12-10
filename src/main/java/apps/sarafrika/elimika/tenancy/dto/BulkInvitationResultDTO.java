package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Summary of a bulk invitation upload run with per-row errors.
 */
public record BulkInvitationResultDTO(

        @JsonProperty("total_rows")
        int totalRows,

        @JsonProperty("successful_invitations")
        int successfulInvitations,

        @JsonProperty("failed_rows")
        int failedRows,

        @JsonProperty("errors")
        List<BulkInvitationRowErrorDTO> errors
) {
}
