package uk.gov.justice.digital.hmpps.incidentreporting.resource

/**
 * Codes that can be used by api clients to uniquely discriminate between error types,
 * instead of relying on non-constant text descriptions of HTTP status codes.
 *
 * NB: Once defined, the values must not be changed
 */
enum class ErrorCode(
  val errorCode: Int,
) {
  ValidationFailure(100),
  ReportNotFound(301),
  ReportAlreadyExists(302),
  ReportModifedInDps(303),
}
