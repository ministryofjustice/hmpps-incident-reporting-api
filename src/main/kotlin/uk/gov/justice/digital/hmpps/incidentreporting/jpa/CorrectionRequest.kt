package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.incidentreporting.dto.CorrectionRequest as CorrectionRequestDto

@Entity
class CorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  private val report: Report,

  val reason: CorrectionReason,
  val descriptionOfChange: String,

  val correctionRequestedBy: String,
  val correctionRequestedAt: LocalDateTime,
) : DtoConvertible {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as CorrectionRequest

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "CorrectionRequest(id=$id)"
  }

  fun getReport() = report

  override fun toDto() = CorrectionRequestDto(
    reason = reason,
    descriptionOfChange = descriptionOfChange,
    correctionRequestedBy = correctionRequestedBy,
    correctionRequestedAt = correctionRequestedAt,
  )
}
