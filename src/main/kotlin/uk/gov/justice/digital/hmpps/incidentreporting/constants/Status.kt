package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class Status(
  val description: String,
) {
  DRAFT("Draft"),
  AWAITING_ANALYSIS("Awaiting analysis"),
  IN_ANALYSIS("In analysis"),
  INFORMATION_REQUIRED("Information required"),
  INFORMATION_AMENDED("Information amened"),
  CLOSED("Closed"),
  POST_INCIDENT_UPDATE("Post-incident update"),
  INCIDENT_UPDATED("Incident updated"),
  DUPLICATE("Duplicate"),
  ;

  companion object {
    fun fromNomisCode(status: String): Status = when (status) {
      "AWAN" -> AWAITING_ANALYSIS
      "INAN" -> IN_ANALYSIS
      "INREQ" -> INFORMATION_REQUIRED
      "INAME" -> INFORMATION_AMENDED
      "CLOSE" -> CLOSED
      "PIU" -> POST_INCIDENT_UPDATE
      "IUP" -> INCIDENT_UPDATED
      "DUP" -> DUPLICATE
      else -> throw ValidationException("Unknown NOMIS incident status: $status")
    }
  }
}
