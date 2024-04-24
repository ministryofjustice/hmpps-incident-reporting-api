package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class HistoricalResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  override val itemValue: String,

  override val recordedBy: String,

  override val recordedOn: LocalDateTime,

  override val additionalInformation: String? = null,
) : GenericResponse
