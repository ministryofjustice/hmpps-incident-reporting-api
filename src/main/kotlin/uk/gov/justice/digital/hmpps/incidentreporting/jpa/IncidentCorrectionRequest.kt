package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class IncidentCorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incident: IncidentReport,

  val correctionRequestedBy: String,

  val correctionRequestedAt: LocalDateTime,

  val reason: CorrectionReason,

  val descriptionOfChange: String? = null,
)

enum class CorrectionReason {
  MISTAKE,
  INCORRECT_INFORMATION,
  MISSING_INFORMATION,
  OTHER,
}
