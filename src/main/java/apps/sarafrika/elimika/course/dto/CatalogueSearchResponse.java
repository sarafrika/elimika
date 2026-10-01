package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.shared.utils.PageMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** One page of the public catalogue: courses and programmes in one ranking, with facet counts. */
@Schema(name = "CatalogueSearchResponse", description = "A page of the public catalogue with facet counts.")
public record CatalogueSearchResponse(
        @JsonProperty("content") List<CatalogueItem> content,
        @Schema(description = "Paging metadata, the same shape as PagedDTO's.")
        @JsonProperty("metadata") PageMetadata metadata,
        @JsonProperty("facets") CatalogueFacets facets
) {
}
