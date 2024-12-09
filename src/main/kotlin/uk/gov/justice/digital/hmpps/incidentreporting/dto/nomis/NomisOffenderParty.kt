package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisOffenderParty(
  @Schema(description = "Offender involved in the incident")
  val offender: NomisOffender,
  @Schema(description = "The sequence number providing an order for a list of offender parties")
  val sequence: Int,
  @Schema(description = "Offender role in the incident")
  val role: NomisCode,
  @Schema(description = "The outcome of the incident")
  val outcome: NomisCode?,
  @Schema(description = "General information about the incident")
  val comment: String?,

  @Schema(description = "The date and time the offender party was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the offender party")
  val createdBy: String,

  @Schema(description = "The date and time the offender party was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @Schema(description = "The username of the person who last updated the offender party")
  val lastModifiedBy: String? = createdBy,
)
