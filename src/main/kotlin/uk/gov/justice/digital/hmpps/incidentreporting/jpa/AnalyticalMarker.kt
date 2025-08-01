package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.helper.EntityOpen
import java.io.Serializable

@Embeddable
data class AnalyticalMarkerPk(
  @Column(name = "response_code")
  val responseCode: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "marker_type")
  val markerType: AnalyticalMarkerType,
) : Serializable

@Entity
@EntityOpen
class AnalyticalMarker(
  @EmbeddedId
  val id: AnalyticalMarkerPk,
)
