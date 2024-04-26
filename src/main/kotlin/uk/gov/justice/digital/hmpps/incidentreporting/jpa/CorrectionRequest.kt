package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import uk.gov.justice.digital.hmpps.incidentreporting.constants.CorrectionReason
import java.time.LocalDateTime

@Entity
class CorrectionRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val report: Report,

  val correctionRequestedBy: String,

  val correctionRequestedAt: LocalDateTime,

  val reason: CorrectionReason,

  val descriptionOfChange: String,
)
