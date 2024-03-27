package uk.gov.justice.digital.hmpps.incidentreporting.dto

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentStatus
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.IncidentType
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Incident Report Details")
data class IncidentReport(
  val id: UUID,
  val incidentNumber: String,
  val incidentType: IncidentType,
  val incidentDateAndTime: LocalDateTime,
  val prisonId: String,

  val incidentDetails: String,

  val reportedBy: String,
  val reportedDate: LocalDateTime,
  val status: IncidentStatus,
  val assignedTo: String? = null,

  val createdDate: LocalDateTime,
  val lastModifiedDate: LocalDateTime,
  val lastModifiedBy: String,

  val createdInNomis: Boolean = false,

)
