package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.dto.Evidence as EvidenceDto

@Entity
class Evidence(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  // TODO: should `type` be an enum?
  val type: String,
  val description: String,
) : DtoConvertible {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as Evidence

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "Evidence(id=$id)"
  }

  fun getReport() = report

  override fun toDto() = EvidenceDto(
    type = type,
    description = description,
  )
}
