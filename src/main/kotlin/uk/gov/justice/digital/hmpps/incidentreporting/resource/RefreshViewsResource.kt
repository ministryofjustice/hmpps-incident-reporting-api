package uk.gov.justice.digital.hmpps.incidentreporting.resource

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.incidentreporting.service.RefreshViewsService

@RestController
@Validated
@RequestMapping("/refresh-views", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Refreshes materialised views",
  description = "Calls the refresh materialised views function in the database",
)
class RefreshViewsResource(
  private val refreshViewsService: RefreshViewsService,
) {
  @PutMapping
  @Operation(
    summary = "Refreshes all materialised views",
    description = "No role required as hidden and can only be run inside the containers",
    hidden = true,
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns 200 on success",
      ),
    ],
  )
  @Hidden
  fun refreshViews() {
    refreshViewsService.refreshViews()
  }
}
