package uk.gov.justice.digital.hmpps.incidentreporting.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import java.time.LocalDateTime

@Schema(description = "Previous statuses an incident report transitioned to", accessMode = Schema.AccessMode.READ_ONLY)
@JsonInclude(JsonInclude.Include.ALWAYS)
data class StatusHistory(
  @Schema(description = "Previous status of an incident report")
  val status: Status,
  @Schema(description = "When the report status was changed", example = "2024-04-29T12:34:56.789012")
  val changedAt: LocalDateTime,
  @Schema(description = "The member of staff who changed the report status")
  val changedBy: String,
) {
  // NB: this property can be removed once fully migrated off NOMIS and reconciliation checks are turned off
  @Suppress("unused")
  @get:Schema(
    description = "Previous NOMIS incident report status code, which may be null for statuses that cannot be mapped",
    nullable = true,
    deprecated = true,
  )
  @get:JsonProperty
  val nomisStatus: String?
    get() = status.nomisStatus
}
