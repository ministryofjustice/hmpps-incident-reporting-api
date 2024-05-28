package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Location as LocationDto

@Entity
@EntityOpen
class Location(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  val locationId: String,

  // TODO: should `type` be an enum?
  val type: String,
  val description: String,
) {
  override fun toString(): String {
    return "Location(id=$id)"
  }

  fun getReport() = report

  fun toDto() = LocationDto(
    locationId = locationId,
    type = type,
    description = description,
  )
}
