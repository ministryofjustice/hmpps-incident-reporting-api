package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisRequirement(
  @Schema(description = "The update required to the incident report")
  val comment: String?,
  @Schema(description = "Date the requirement was recorded")
  val date: LocalDate,
  @Schema(description = "The staff member who made the requirement request")
  val staff: NomisStaff,
  @Schema(description = "The reporting location of the staff")
  val prisonId: String,

  @Schema(description = "The date and time the requirement was created")
  val createDateTime: LocalDateTime,
  @Schema(description = "The username of the person who created the requirement")
  val createdBy: String,

  @Schema(description = "The date and time the requirement was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @Schema(description = "The username of the person who last updated the requirement")
  val lastModifiedBy: String? = createdBy,
)
