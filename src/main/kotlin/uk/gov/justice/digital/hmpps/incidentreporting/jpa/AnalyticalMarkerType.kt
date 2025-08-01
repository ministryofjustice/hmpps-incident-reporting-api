package uk.gov.justice.digital.hmpps.incidentreporting.jpa

enum class AnalyticalMarkerType(
  val markerDescription: String,
) {
  SERIOUS_INJURY("Serious injury sustained"),
  SEXUAL_ASSAULT("Sexual assault"),
  MEDICAL_TREATMENT_REQUIRED("Medical treatment required"),
  HOSPITAL_ADMISSION("Hospital admission"),
  TRANSMITTABLE_INFECTIOUS_DISEASE("Transmittable infectious disease"),
  SALIVA_HIT_BODY("Saliva hit body"),
  HIT_NECK_OR_ABOVE("Hit neck or above"),
  WEAPON_URINE("Urine"),
}
