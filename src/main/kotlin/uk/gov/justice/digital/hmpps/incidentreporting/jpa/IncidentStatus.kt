package uk.gov.justice.digital.hmpps.incidentreporting.jpa

enum class IncidentStatus {
  DRAFT,
  AWAITING_ANALYSIS,
  IN_ANALYSIS,
  INFORMATION_REQUIRED,
  INFORMATION_AMENDED,
  CLOSED,
  POST_INCIDENT_UPDATE,
  INCIDENT_UPDATED,
  DUPLICATE,
}
