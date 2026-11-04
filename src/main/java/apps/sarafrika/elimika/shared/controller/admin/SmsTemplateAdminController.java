package apps.sarafrika.elimika.shared.controller.admin;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.sms.template.SmsTemplateResponse;
import apps.sarafrika.elimika.shared.sms.template.SmsTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sms-templates")
@Tag(name = "Admin SMS Templates", description = "Wording of the SMS the platform sends")
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class SmsTemplateAdminController {

    private final SmsTemplateService service;
    private final DomainSecurityService domainSecurityService;
    private final UserLookupService userLookupService;

    @GetMapping
    @Operation(summary = "List every system SMS with its wording and allowed placeholders")
    public ResponseEntity<ApiResponse<List<SmsTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.list().stream().map(SmsTemplateResponse::of).toList(),
                "SMS templates retrieved"));
    }

    @PutMapping("/{key}")
    @Operation(summary = "Save new wording; rejected unless it passes the placeholder and length checks")
    public ResponseEntity<ApiResponse<SmsTemplateResponse>> save(@PathVariable String key,
                                                                 @RequestBody SmsTemplateResponse.UpdateRequest request) {
        UUID me = domainSecurityService.getCurrentUserUuid();
        var view = service.save(key, request.body(), request.enabled() == null || request.enabled(),
                me == null ? null : userLookupService.getUserFullName(me).orElse(null));
        return ResponseEntity.ok(ApiResponse.success(SmsTemplateResponse.of(view), "SMS template saved"));
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Go back to the built-in wording")
    public ResponseEntity<ApiResponse<SmsTemplateResponse>> reset(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success(SmsTemplateResponse.of(service.reset(key)),
                "SMS template reset to default"));
    }

    @PostMapping("/{key}/test")
    @Operation(summary = "Send the saved wording with sample values to the admin's own phone")
    public ResponseEntity<ApiResponse<SmsTemplateResponse.TestResponse>> test(@PathVariable String key) {
        UUID me = domainSecurityService.getCurrentUserUuid();
        var sent = service.sendTest(key, me == null ? null : userLookupService.getUserPhoneNumber(me).orElse(null));
        return ResponseEntity.ok(ApiResponse.success(new SmsTemplateResponse.TestResponse(sent.sent(), sent.toMasked()),
                sent.sent() ? "Test SMS sent" : "The SMS provider did not accept the test"));
    }
}
