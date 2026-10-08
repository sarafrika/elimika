package apps.sarafrika.elimika.notifications.controller;

import apps.sarafrika.elimika.notifications.api.NotificationResult;
import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.notifications.service.EmailNotificationService;
import apps.sarafrika.elimika.notifications.template.EmailSamples;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sends sample copies of Elimika's emails to one address so admins can check delivery end to end. */
@RestController
@RequestMapping("/api/v1/admin/notifications/email-test")
@RequiredArgsConstructor
@Tag(name = "Admin Email Test", description = "Send sample copies of every Elimika email to a test address")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class AdminEmailTestController {

    private final EmailNotificationService emailNotificationService;

    public record EmailTestRequest(
            @NotBlank @Email @JsonProperty("to") String to,
            @JsonProperty("types") List<String> types
    ) {
    }

    public record EmailTestResult(
            @JsonProperty("type") String type,
            @JsonProperty("template") String template,
            @JsonProperty("sent") boolean sent,
            @JsonProperty("error") String error
    ) {
    }

    @Operation(operationId = "listTestableEmails", summary = "List the notification types that have an email template")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> types() {
        return ResponseEntity.ok(ApiResponse.success(
                EmailSamples.typesWithTemplates().stream().map(NotificationType::getValue).toList(),
                "Email types retrieved successfully"));
    }

    @Operation(operationId = "sendTestEmails", summary = "Send sample emails to a test address",
            description = "Sends one sample of each requested type (all templated types when none are given) "
                    + "through the configured mail server and reports each delivery.")
    @PostMapping
    public ResponseEntity<ApiResponse<List<EmailTestResult>>> send(@Valid @RequestBody EmailTestRequest request) {
        List<NotificationType> types = request.types() == null || request.types().isEmpty()
                ? EmailSamples.typesWithTemplates()
                : request.types().stream().map(type -> NotificationType.fromValue(type.toUpperCase(Locale.ROOT))).toList();
        List<EmailTestResult> results = new ArrayList<>();
        for (NotificationType type : types) {
            NotificationResult result = emailNotificationService
                    .sendEmail(EmailSamples.event(type, request.to(), EmailSamples.sampleVariables()))
                    .join();
            results.add(new EmailTestResult(type.getValue(), type.getTemplateName(), result.isSuccessful(),
                    result.isSuccessful() ? null : result.errorMessage()));
        }
        long sent = results.stream().filter(EmailTestResult::sent).count();
        return ResponseEntity.ok(ApiResponse.success(results, sent + " of " + results.size() + " test emails sent"));
    }
}
