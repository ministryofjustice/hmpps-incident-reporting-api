package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderColumn

@Entity
class HistoricalIncidentResponse(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  val incident: IncidentReport,

  val dataItem: String,

  val dataItemDescription: String? = null,

  @OneToMany(mappedBy = "incidentResponse", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @OrderColumn(name = "sequence")
  val responses: MutableList<HistoricalResponse> = mutableListOf(),

  @OneToOne(fetch = FetchType.LAZY)
  val evidence: Evidence? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val location: IncidentLocation? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val otherPersonInvolvement: OtherPersonInvolvement? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val prisonerInvolvement: PrisonerInvolvement? = null,

  @OneToOne(fetch = FetchType.LAZY)
  val staffInvolvement: StaffInvolvement? = null,

)
