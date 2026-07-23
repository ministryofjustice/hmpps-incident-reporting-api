package uk.gov.justice.digital.hmpps.incidentreporting.constants

/**
 * The status of an incident report.
 *
 * NB:
 *   - new items should have a reasonably readable code
 *   - items cannot be removed to preserve database integrity
 *   - any additions, changes to order, codes, descriptions or definitions require a new migration of relevant
 *     constants DB table!
 *   - code & description are expected to be 60 chars max
 *   - definition is expected to be 255 chars max
 */
enum class Status(
  val description: String,
  val definition: String,
  val nomisStatus: String?,
  val ignoreDownstream: Boolean = false,
) {
  DRAFT("Draft", "A report that has been created but not yet submitted.", null),
  AWAITING_REVIEW(
    "Awaiting review",
    "A report that has been submitted for the first time and is now ready for review.",
    "AWAN",
  ),
  ON_HOLD("On hold", "The report has been set aside for investigation and cannot be edited at this time.", "INAN"),
  NEEDS_UPDATING("Needs updating", "A report that has been submitted and sent back for updates.", "INREQ"),
  UPDATED("Updated", "A report that has been submitted more than one time and ready for review.", "INAME"),
  CLOSED("Closed", "A report that has been submitted, reviewed and is now complete.", "CLOSE"),

  // the following statuses should be ignored downstream for most statistical purposes
  DUPLICATE("Duplicate", "A copy of a report that already exists.", "DUP", true),
  NOT_REPORTABLE("Not reportable", "A report for an incident that did not need to be created.", null, true),
  REOPENED("Reopened", "A report that has been reopened after it was closed/completed.", null, true),
  WAS_CLOSED("Was closed", "A report that has been reopened from completed and then resubmitted.", null, true),
}
