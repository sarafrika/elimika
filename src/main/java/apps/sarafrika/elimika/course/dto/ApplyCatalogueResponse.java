package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.utils.PageMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** One page of the instructor apply-to-train catalogue: courses and programmes in one ranking. */
@Schema(name = "ApplyCatalogueResponse", description = "A page of the apply-to-train catalogue with facet counts.")
public record ApplyCatalogueResponse(
        @JsonProperty("content") List<ApplyCatalogueItem> content,
        @Schema(description = "Paging metadata, the same shape as PagedDTO's.")
        @JsonProperty("metadata") PageMetadata metadata,
        @JsonProperty("facets") ApplyCatalogueFacets facets
) {
}
