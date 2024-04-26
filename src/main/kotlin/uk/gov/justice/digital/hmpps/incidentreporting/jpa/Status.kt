package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.validation.ValidationException

enum class Status {
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
    "AWAN" -> Status.AWAITING_ANALYSIS
    "INAN" -> Status.IN_ANALYSIS
    "INREQ" -> Status.INFORMATION_REQUIRED
    "INAME" -> Status.INFORMATION_AMENDED
    "CLOSE" -> Status.CLOSED
    "PIU" -> Status.POST_INCIDENT_UPDATE
    "IUP" -> Status.INCIDENT_UPDATED
    "DUP" -> Status.DUPLICATE
    else -> throw ValidationException("Unknown incident status: $code")
  }
