package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisStaffParty(
  @Schema(description = "Staff involved in the incident")
  val staff: NomisStaff,
  @Schema(description = "The sequence number providing an order for a list of staff parties")
  val sequence: Int? = null,
  @Schema(description = "Staff role in the incident")
  val role: NomisCode,
  @Schema(description = "General information about the incident")
  val comment: String?,

  @Schema(description = "The date and time the staff party was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the staff party")
  val createdBy: String,

  @Schema(description = "The date and time the staff party was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @Schema(description = "The username of the person who last updated the staff party")
  val lastModifiedBy: String? = createdBy,
)
