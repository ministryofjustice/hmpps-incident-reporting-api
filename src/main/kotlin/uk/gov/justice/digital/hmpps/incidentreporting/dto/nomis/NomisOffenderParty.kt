package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisOffenderParty(
  @param:Schema(description = "Offender involved in the incident")
  val offender: NomisOffender,
  @param:Schema(description = "The sequence number providing an order for a list of offender parties")
  val sequence: Int,
  @param:Schema(description = "Offender role in the incident")
  val role: NomisCode,
  @param:Schema(description = "The outcome of the incident")
  val outcome: NomisCode?,
  @param:Schema(description = "General information about the incident")
  val comment: String?,

  @param:Schema(description = "The date and time the offender party was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the offender party")
  val createdBy: String,

  @param:Schema(description = "The date and time the offender party was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @param:Schema(description = "The username of the person who last updated the offender party")
  val lastModifiedBy: String? = createdBy,
)
