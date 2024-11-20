package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

enum class Status(
  val description: String,
  val nomisStatus: String?,
  val deprecated: Boolean = false,
) {
  DRAFT("Draft", null),
  AWAITING_ANALYSIS("Awaiting analysis", "AWAN"),
  IN_ANALYSIS("In analysis", "INAN"),
  INFORMATION_REQUIRED("Information required", "INREQ"),
  INFORMATION_AMENDED("Information amened", "INAME"),
  CLOSED("Closed", "CLOSE"),
  POST_INCIDENT_UPDATE("Post-incident update", "PIU", true),
  INCIDENT_UPDATED("Incident updated", "IUP", true),
  DUPLICATE("Duplicate", "DUP"),
  REPORTED_IN_ERROR("Reported in error", null),
  ;

  companion object {
    fun fromNomisCode(status: String): Status = Status.entries.find { it.nomisStatus == status }
      ?: throw ValidationException("Unknown NOMIS incident status: $status")
  }
}
