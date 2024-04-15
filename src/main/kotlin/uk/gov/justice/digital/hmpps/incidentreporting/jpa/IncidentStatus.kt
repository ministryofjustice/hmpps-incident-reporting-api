package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

enum class IncidentStatus {
  DRAFT,
  AWAITING_ANALYSIS,
  IN_ANALYSIS,
  INFORMATION_REQUIRED,
  INFORMATION_AMENDED,
  CLOSED,
  POST_INCIDENT_UPDATE,
  INCIDENT_UPDATED,
  DUPLICATE,
}

fun mapIncidentStatus(code: String) =
  when (code) {
    "AWAN" -> IncidentStatus.AWAITING_ANALYSIS
    "INAN" -> IncidentStatus.IN_ANALYSIS
    "INREQ" -> IncidentStatus.INFORMATION_REQUIRED
    "INAME" -> IncidentStatus.INFORMATION_AMENDED
    "CLOSE" -> IncidentStatus.CLOSED
    "PIU" -> IncidentStatus.POST_INCIDENT_UPDATE
    "IUP" -> IncidentStatus.INCIDENT_UPDATED
    "DUP" -> IncidentStatus.DUPLICATE
    else -> throw ValidationException("Unknown incident status: $code")
  }
