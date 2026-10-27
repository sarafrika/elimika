package apps.sarafrika.elimika.course.controller;

import apps.sarafrika.elimika.course.dto.ClassProgrammeProgressDTO;
import apps.sarafrika.elimika.course.dto.ProgramRemarkDTO;
import apps.sarafrika.elimika.course.dto.ProgramReportDTO;
import apps.sarafrika.elimika.course.dto.ProgramReportSummaryDTO;
import apps.sarafrika.elimika.course.internal.ProgramReportAccess;
import apps.sarafrika.elimika.course.service.ProgramReportService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Learner programme reports, instructor remarks and class programme progress. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Programme Reports", description = "Live programme marks, skills and instructor remarks")
public class ProgramReportController {

    private static final String READ = "@programReportAccess.canRead(#programUuid, #studentUuid) or @domainSecurityService.isPlatformAdmin()";

    private final ProgramReportService programReportService;
    private final DomainSecurityService domainSecurityService;
    private final ProgramReportAccess programReportAccess;

    @Operation(operationId = "listStudentProgramReports", summary = "List the programmes whose reports the caller may open for a learner",
            description = "One row per programme the learner is enrolled in, limited to programmes whose report the caller can read.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/programs/students/{studentUuid}/reports")
    public ResponseEntity<ApiResponse<List<ProgramReportSummaryDTO>>> listForStudent(@PathVariable UUID studentUuid) {
        boolean admin = domainSecurityService.isPlatformAdmin();
        List<ProgramReportSummaryDTO> readable = programReportService.listForStudent(studentUuid).stream()
                .filter(summary -> admin || programReportAccess.canRead(summary.programUuid(), studentUuid))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(readable, "Programme reports listed"));
    }

    @Operation(operationId = "getProgramReport", summary = "Get a learner's programme report",
            description = "Programme mark (provisional until every mandatory course is decided), pass checks, course rows with "
                    + "attendance in the period, skill levels with evidence history, and remarks. Readable by the learner, "
                    + "a guardian sharing academics and the learner's programme class instructors.")
    @PreAuthorize(READ)
    @GetMapping("/api/v1/programs/{programUuid}/students/{studentUuid}/report")
    public ResponseEntity<ApiResponse<ProgramReportDTO>> getReport(
            @PathVariable UUID programUuid, @PathVariable UUID studentUuid,
            @Parameter(description = "First day of the period (UTC)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day of the period (UTC)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(programReportService.getReport(programUuid, studentUuid, from, to),
                "Programme report retrieved"));
    }

    @Operation(operationId = "getProgramRemarks", summary = "List a learner's programme remarks", description = "Newest first.")
    @PreAuthorize(READ)
    @GetMapping("/api/v1/programs/{programUuid}/students/{studentUuid}/remarks")
    public ResponseEntity<ApiResponse<List<ProgramRemarkDTO>>> getRemarks(@PathVariable UUID programUuid, @PathVariable UUID studentUuid) {
        return ResponseEntity.ok(ApiResponse.success(programReportService.getRemarks(programUuid, studentUuid), "Remarks retrieved"));
    }

    @Operation(operationId = "addProgramRemark", summary = "Add a programme progress remark",
            description = "Rich-text strengths, areas to improve and recommendations, plus a next review date. "
                    + "Only the default instructor of one of the learner's programme classes may add one; earlier remarks are kept.")
    @PreAuthorize("@programReportAccess.canWriteRemarks(#programUuid, #studentUuid)")
    @PostMapping("/api/v1/programs/{programUuid}/students/{studentUuid}/remarks")
    public ResponseEntity<ApiResponse<ProgramRemarkDTO>> addRemark(@PathVariable UUID programUuid, @PathVariable UUID studentUuid,
                                                                 @Valid @RequestBody ProgramRemarkDTO remark) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                programReportService.addRemark(programUuid, studentUuid, remark, domainSecurityService.getCurrentUserUuid()),
                "Remark saved"));
    }

    @Operation(operationId = "getClassProgrammeProgress", summary = "Get a class's programme progress",
            description = "Programme marks and skill levels for the class's learners, with per-skill attainment counts. "
                    + "Pass program_uuid when the class's course sits in more than one programme.")
    @PreAuthorize("@programReportAccess.canReadClass(#classUuid) or @domainSecurityService.isPlatformAdmin()")
    @GetMapping("/api/v1/classes/{classUuid}/programme-progress")
    public ResponseEntity<ApiResponse<ClassProgrammeProgressDTO>> getClassProgress(
            @PathVariable UUID classUuid,
            @RequestParam(name = "program_uuid", required = false) UUID programUuid) {
        return ResponseEntity.ok(ApiResponse.success(programReportService.getClassProgress(classUuid, programUuid),
                "Class programme progress retrieved"));
    }
}
