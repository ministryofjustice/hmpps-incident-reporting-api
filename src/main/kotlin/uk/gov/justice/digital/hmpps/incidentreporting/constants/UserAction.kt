package uk.gov.justice.digital.hmpps.incidentreporting.constants

/**
 * Actions users can perform on a report as documented as part of correction requests
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class UserAction(
  val description: String,
) {
  REQUEST_REVIEW("Request a data warden to review the report"),
  REQUEST_DUPLICATE("Request to mark duplicate"),
  REQUEST_NOT_REPORTABLE("Request to mark not reportable"),
  REQUEST_CORRECTION("Request correction"),
  RECALL("Recall"),
  CLOSE("Close"),
  MARK_DUPLICATE("Mark as duplicate"),
  MARK_NOT_REPORTABLE("Mark as not reportable"),
  HOLD("Put on hold"),
}
