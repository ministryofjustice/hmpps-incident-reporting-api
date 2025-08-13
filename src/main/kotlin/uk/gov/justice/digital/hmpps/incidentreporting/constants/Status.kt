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
  AWAITING_REVIEW("Awaiting review", "AWAN"),
  ON_HOLD("On hold", "INAN"),
  NEEDS_UPDATING("Needs updating", "INREQ"),
  UPDATED("Updated", "INAME"),
  CLOSED("Closed", "CLOSE"),

  DUPLICATE("Duplicate", "DUP"),
  NOT_REPORTABLE("Not reportable", null),
  REOPENED("Reopened", null),
  WAS_CLOSED("Was closed", null),
  ;

  companion object {
    fun fromNomisCode(status: String): Status = Status.entries.find { it.nomisStatus == status }
      ?: throw ValidationException("Unknown NOMIS incident status: $status")
  }
}
