package apps.sarafrika.elimika.shared.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.dto.DocumentTypeOptionDTO;
import apps.sarafrika.elimika.shared.service.DocumentTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/document-types")
@Tag(name = "Document Types", description = "Lookup endpoints for upload document type options")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeService documentTypeService;

    @GetMapping
    @Operation(summary = "List available document types",
            description = "Filter with applies_to to get the checklist for one onboarding flow, "
                    + "for example ORGANISATION. Omitting it returns the whole catalogue.")
    public ResponseEntity<ApiResponse<List<DocumentTypeOptionDTO>>> listDocumentTypes(
            @Parameter(description = "Onboarding flow the checklist is for", example = "ORGANISATION")
            @RequestParam(value = "applies_to", required = false) String appliesTo) {
        List<DocumentTypeOptionDTO> documentTypes = documentTypeService.listDocumentTypes(appliesTo);
        return ResponseEntity.ok(ApiResponse.success(documentTypes, "Document types retrieved successfully"));
    }
}
