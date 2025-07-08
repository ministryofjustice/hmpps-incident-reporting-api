package uk.gov.justice.digital.hmpps.incidentreporting.dto.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NomisRequirement(
  @param:Schema(description = "The sequence number providing an order for a list of requirements")
  val sequence: Int,
  @param:Schema(description = "The update required to the incident report")
  val comment: String?,
  @param:Schema(description = "Date and time the requirement was recorded")
  val recordedDate: LocalDateTime,
  @param:Schema(description = "The staff member who made the requirement request")
  val staff: NomisStaff,
  @param:Schema(description = "The reporting location of the staff")
  val prisonId: String,

  @param:Schema(description = "The date and time the requirement was created")
  val createDateTime: LocalDateTime,
  @param:Schema(description = "The username of the person who created the requirement")
  val createdBy: String,

  @param:Schema(description = "The date and time the requirement was last updated")
  val lastModifiedDateTime: LocalDateTime? = createDateTime,
  @param:Schema(description = "The username of the person who last updated the requirement")
  val lastModifiedBy: String? = createdBy,
)
