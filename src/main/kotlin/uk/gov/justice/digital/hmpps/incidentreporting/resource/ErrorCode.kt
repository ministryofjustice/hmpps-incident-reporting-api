package uk.gov.justice.digital.hmpps.incidentreporting.resource

/**
 * Codes that can be used by api clients to uniquely discriminate between error types,
 * instead of relying on non-constant text descriptions of HTTP status codes.
 *
 * NB: Once defined, the values must not be changed
 */
enum class ErrorCode(val errorCode: Int) {
  IncidentReportNotFound(101),
  ValidationFailure(102),
  IncidentReportAlreadyExists(103),
}
