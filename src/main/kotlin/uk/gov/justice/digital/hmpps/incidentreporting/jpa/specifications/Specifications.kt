package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import org.springframework.data.jpa.domain.Specification
import kotlin.reflect.KProperty1

/** Creates an equality specification from an entity’s property */
fun <T, V> KProperty1<T, V>.buildSpecForEqualTo(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<V>(name), value)
  }
}
