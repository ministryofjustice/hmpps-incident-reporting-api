package uk.gov.justice.digital.hmpps.incidentreporting.constants

import jakarta.validation.ValidationException

/**
 * The status of an incident report.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class Status(
  val description: String,
  val nomisStatus: String?,
) {
  DRAFT("Draft", null),
  AWAITING_ANALYSIS("Awaiting analysis", "AWAN"),
  IN_ANALYSIS("In analysis", "INAN"),
  INFORMATION_REQUIRED("Information required", "INREQ"),
  INFORMATION_AMENDED("Information amended", "INAME"),
  CLOSED("Closed", "CLOSE"),
  POST_INCIDENT_UPDATE("Post-incident update", "PIU"),
  INCIDENT_UPDATED("Incident updated", "IUP"),
  DUPLICATE("Duplicate", "DUP"),
  ;

  companion object {
    fun fromNomisCode(status: String): Status = Status.entries.find { it.nomisStatus == status }
      ?: throw ValidationException("Unknown NOMIS incident status: $status")
  }
}
