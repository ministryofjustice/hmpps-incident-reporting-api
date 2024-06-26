package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.dto.request.UpdateEvidence
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Evidence as EvidenceDto

@Entity
class Evidence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  // TODO: should `type` be an enum?
  var type: String,
  var description: String,
) {
  override fun toString(): String {
    return "Evidence(id=$id)"
  }

  fun getReport() = report

  fun updateWith(request: UpdateEvidence) {
    request.type?.let { type = it }
    request.description?.let { description = it }
  }

  fun toDto() = EvidenceDto(
    type = type,
    description = description,
  )
}
