package uk.gov.justice.digital.hmpps.incidentreporting.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Status
import uk.gov.justice.digital.hmpps.incidentreporting.constants.Type
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident report")
data class Report(
  val id: UUID,
  val incidentNumber: String,
  val type: Type,
  val incidentDateAndTime: LocalDateTime,
  val prisonId: String,
  val title: String,
  val description: String,
  val event: Event,

  val reportedBy: String,
  val reportedDate: LocalDateTime,
  val status: Status,
  val assignedTo: String? = null,

  val createdDate: LocalDateTime,
  val lastModifiedDate: LocalDateTime,
  val lastModifiedBy: String,

  val createdInNomis: Boolean = false,
)
