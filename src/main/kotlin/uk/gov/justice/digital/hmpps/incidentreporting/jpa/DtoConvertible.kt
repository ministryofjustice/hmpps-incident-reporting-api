package uk.gov.justice.digital.hmpps.incidentreporting.jpa

import uk.gov.justice.digital.hmpps.incidentreporting.dto.Dto
import java.io.Serializable

sealed interface DtoConvertible : Serializable {
  fun toDto(): Dto
}
