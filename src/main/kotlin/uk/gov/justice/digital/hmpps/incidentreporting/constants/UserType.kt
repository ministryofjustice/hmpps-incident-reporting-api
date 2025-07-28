package uk.gov.justice.digital.hmpps.incidentreporting.constants

/**
 * Type of user performing actions on a report when submitting correction requests
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes or descriptions require a new migration of relevant constants DB table!
 *   - code & description are expected to be 60 chars max
 */
enum class UserType(
  val description: String,
) {
  REPORTING_OFFICER("Reporting officer"),
  DATA_WARDEN("Data warden"),
}
