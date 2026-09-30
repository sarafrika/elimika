package apps.sarafrika.elimika.shared.tracking.web;

import apps.sarafrika.elimika.shared.service.UserContextService;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryEventType;
import apps.sarafrika.elimika.shared.tracking.service.DiscoveryEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery/events")
@RequiredArgsConstructor
@Tag(name = "Discovery", description = "Feedback on recommended items")
public class DiscoveryEventController {

    private final DiscoveryEventService discoveryEventService;
    private final UserContextService userContextService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(operationId = "recordDiscoveryEvent", summary = "Record a click or dismissal of a recommended item",
            description = "Reports that the signed-in user clicked or dismissed an item from a recommendation response. "
                    + "The user is always the authenticated principal. The event is kept only when it matches an item "
                    + "that response really showed this user; otherwise it is dropped silently. Always 202 for a "
                    + "well-formed request. Events are kept for 180 days.")
    @ApiResponse(responseCode = "202", description = "Accepted")
    @ApiResponse(responseCode = "400", description = "Malformed body, or an event_type other than CLICK or DISMISS")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> record(@Valid @RequestBody DiscoveryEventRequest request) {
        discoveryEventService.recordClientEvent(
                userContextService.getCurrentUserUuid(),
                request.recommendationId(),
                request.itemUuid(),
                request.itemType(),
                DiscoveryEventType.fromValue(request.eventType()),
                request.position());
        return ResponseEntity.accepted().build();
    }
}
