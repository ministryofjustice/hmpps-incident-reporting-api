package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateLocation
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Location as LocationDto

@Entity
class Location(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  var locationId: String,

  // TODO: should `type` be an enum?
  var type: String,
  var description: String,
) {
  override fun toString(): String {
    return "Location(id=$id)"
  }

  fun getReport() = report

  fun updateWith(request: UpdateLocation) {
    request.locationId?.let { locationId = it }
    request.type?.let { type = it }
    request.description?.let { description = it }
  }

  fun toDto() = LocationDto(
    locationId = locationId,
    type = type,
    description = description,
  )
}
