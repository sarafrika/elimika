package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.dto.NotificationDispatchDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationNotificationRequestDTO;
import apps.sarafrika.elimika.tenancy.services.OrganisationNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organisation Notifications API",
        description = "Outgoing notifications an organisation broadcasts to its members")
public class OrganisationNotificationController {

    private static final String READ_ORGANISATION =
            "@organisationSecurityService.canReadOrganisation(#organisationUuid)";
    private static final String MANAGE_ORGANISATION =
            "@organisationSecurityService.canManageOrganisation(#organisationUuid)";

    private final OrganisationNotificationService organisationNotificationService;
    private final DomainSecurityService domainSecurityService;

    @Operation(summary = "Broadcast a notification to an organisation's members",
            description = "Sends an in-app notification (and an email when the channel is email) to the "
                    + "requested audience, and records the send.")
    @PostMapping("/organisations/{organisationUuid}/notifications")
    @PreAuthorize(MANAGE_ORGANISATION)
    public ResponseEntity<ApiResponse<NotificationDispatchDTO>> send(
            @Parameter(description = "UUID of the organisation sending the notification", required = true)
            @PathVariable UUID organisationUuid,
            @Valid @RequestBody SendOrganisationNotificationRequestDTO request) {

        UUID sender = domainSecurityService.getCurrentUserUuid();
        NotificationDispatchDTO dispatch =
                organisationNotificationService.send(organisationUuid, request, sender);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(dispatch, "Notification sent to "
                        + dispatch.recipientCount() + " recipient(s)"));
    }

    @Operation(summary = "List an organisation's sent notifications",
            description = "The organisation's outgoing broadcasts, newest first.")
    @GetMapping("/organisations/{organisationUuid}/notifications/sent")
    @PreAuthorize(READ_ORGANISATION)
    public ResponseEntity<ApiResponse<List<NotificationDispatchDTO>>> listSent(
            @Parameter(description = "UUID of the organisation", required = true)
            @PathVariable UUID organisationUuid,
            @Parameter(description = "Maximum number of dispatches to return (1-100)")
            @RequestParam(defaultValue = "20") int limit) {

        List<NotificationDispatchDTO> sent =
                organisationNotificationService.listSent(organisationUuid, limit);
        return ResponseEntity.ok(ApiResponse.success(sent, "Sent notifications retrieved successfully"));
    }
}
