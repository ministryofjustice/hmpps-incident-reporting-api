package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class PrisonerInvolvement(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incident: IncidentReport,

  val prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  val prisonerInvolvement: PrisonerRole,

  @Enumerated(EnumType.STRING)
  val outcome: PrisonerOutcome? = null,

  val comment: String? = null,
)
