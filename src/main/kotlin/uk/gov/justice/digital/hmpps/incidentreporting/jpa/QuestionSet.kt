package uk.gov.justice.digital.hmpps.incidentreporting.jpa

enum class QuestionSet(
  type: IncidentType,
) {
  ASSAULT(
    IncidentType.ASSAULT,
  ),
  FIND(
    IncidentType.FIND,
  ),
  SELF_HARM_V1(
    IncidentType.SELF_HARM,
  ),
  DRONE(
    IncidentType.DRONE,
  ),
}
