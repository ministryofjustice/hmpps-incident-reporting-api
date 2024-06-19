package uk.gov.justice.digital.hmpps.incidentreporting.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type

@Schema(description = "Changes an incident report’s type")
data class ChangeTypeRequest(
  @Schema(description = "The new type", required = true, example = "DAMAGE")
  val newType: Type,
) {
  fun validate() {
    if (!newType.active) {
      throw ValidationException("Inactive incident type $newType")
    }
  }
}
