package apps.sarafrika.elimika.notifications.controller;

import apps.sarafrika.elimika.notifications.api.NotificationPresentation;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.api.UserNotificationStatus;
import apps.sarafrika.elimika.notifications.dto.NotificationActionResultDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationCountsDTO;
import apps.sarafrika.elimika.notifications.dto.NotificationDTO;
import apps.sarafrika.elimika.notifications.service.UserNotificationService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.PagedDTO;
import apps.sarafrika.elimika.shared.service.UserContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping(NotificationController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Authenticated user notification inbox and actions")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    public static final String API_ROOT_PATH = "/api/v1/notifications";

    private final UserNotificationService userNotificationService;
    private final UserContextService userContextService;

    @GetMapping
    @Operation(summary = "List current user's notifications")
    public ResponseEntity<ApiResponse<PagedDTO<NotificationDTO>>> listNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String presentation,
            @RequestParam(required = false) String type,
            @RequestParam(name = "popup_seen", required = false) Boolean popupSeen,
            Pageable pageable
    ) {
        UUID recipientUuid = userContextService.getCurrentUserUuid();
        Page<NotificationDTO> notifications = userNotificationService.listNotifications(
                recipientUuid,
                parseStatus(status),
                parsePresentation(presentation),
                parseType(type),
                popupSeen,
                pageable
        );
        String baseUrl = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
        return ResponseEntity.ok(ApiResponse.success(
                PagedDTO.from(notifications, baseUrl),
                "Notifications retrieved successfully"
        ));
    }

    @GetMapping("/counts")
    @Operation(summary = "Get current user's notification counts")
    public ResponseEntity<ApiResponse<NotificationCountsDTO>> getCounts() {
        NotificationCountsDTO counts = userNotificationService.getCounts(userContextService.getCurrentUserUuid());
        return ResponseEntity.ok(ApiResponse.success(counts, "Notification counts retrieved successfully"));
    }

    @PostMapping("/{uuid}")
    @Operation(summary = "Apply an action to one notification")
    public ResponseEntity<ApiResponse<NotificationDTO>> applyAction(
            @PathVariable UUID uuid,
            @RequestParam("action") String action
    ) {
        NotificationDTO notification = userNotificationService.applyAction(
                userContextService.getCurrentUserUuid(),
                uuid,
                action
        );
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification action completed successfully"));
    }

    @PostMapping
    @Operation(summary = "Apply a bulk notification action")
    public ResponseEntity<ApiResponse<NotificationActionResultDTO>> applyBulkAction(
            @RequestParam("action") String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String presentation,
            @RequestParam(required = false) String type
    ) {
        NotificationActionResultDTO result = userNotificationService.applyBulkAction(
                userContextService.getCurrentUserUuid(),
                action,
                parseStatus(status),
                parsePresentation(presentation),
                parseType(type)
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Notification bulk action completed successfully"));
    }

    private UserNotificationStatus parseStatus(String value) {
        return value == null ? null : UserNotificationStatus.fromValue(value.toUpperCase(Locale.ROOT));
    }

    private NotificationPresentation parsePresentation(String value) {
        return value == null ? null : NotificationPresentation.fromValue(value.toUpperCase(Locale.ROOT));
    }

    private NotificationType parseType(String value) {
        return value == null ? null : NotificationType.fromValue(value.toUpperCase(Locale.ROOT));
    }
}
