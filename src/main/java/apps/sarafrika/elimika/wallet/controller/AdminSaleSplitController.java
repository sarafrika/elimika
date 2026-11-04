package apps.sarafrika.elimika.wallet.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.wallet.dto.SaleSplitDTO;
import apps.sarafrika.elimika.wallet.service.SaleSplitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** How each captured sale line was split, for platform admins: both parties, the fees and anything retained. */
@RestController
@RequestMapping("/api/v1/admin/sales")
@Tag(name = "Admin sales", description = "The split of each captured sale line")
@RequiredArgsConstructor
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
public class AdminSaleSplitController {

    private final SaleSplitService saleSplitService;

    @GetMapping("/orders/{orderId}/splits")
    @Operation(operationId = "listAdminOrderSaleSplits", summary = "List the split of each settled line of an order",
            description = "In line order. Both parties are named, with their owner uuids and wallet rows.")
    public ResponseEntity<ApiResponse<List<SaleSplitDTO>>> splitsOfOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.success(saleSplitService.splitsOfOrder(orderId),
                "Order sale splits retrieved successfully"));
    }

    @GetMapping("/splits/{splitUuid}")
    @Operation(operationId = "getAdminSaleSplit", summary = "Get one sale split")
    public ResponseEntity<ApiResponse<SaleSplitDTO>> split(@PathVariable UUID splitUuid) {
        return ResponseEntity.ok(ApiResponse.success(saleSplitService.split(splitUuid),
                "Sale split retrieved successfully"));
    }
}
