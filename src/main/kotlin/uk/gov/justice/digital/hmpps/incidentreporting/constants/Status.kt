package uk.gov.justice.digital.hmpps.incidentreporting.constants

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
}
