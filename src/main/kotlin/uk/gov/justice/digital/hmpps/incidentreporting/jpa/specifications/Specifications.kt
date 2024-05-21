package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import org.springframework.data.jpa.domain.Specification
import kotlin.reflect.KProperty1

/** Build «equal to» specification from an entity’s property */
fun <T, V> KProperty1<T, V>.buildSpecForEqualTo(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.equal(root.get<V>(name), value)
  }
}

/** Build «in» specification from an entity’s property */
fun <T, V> KProperty1<T, V>.buildSpecForIn(values: Collection<V>): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.and(root.get<V>(name).`in`(values))
  }
}

/** Build «less than» specification from an entity’s property */
fun <T, V : Comparable<V>> KProperty1<T, V>.buildSpecForLessThan(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.lessThan(root.get(name), value)
  }
}

/** Build «less than or equal to» specification from an entity’s property */
fun <T, V : Comparable<V>> KProperty1<T, V>.buildSpecForLessThanOrEqualTo(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.lessThanOrEqualTo(root.get(name), value)
  }
}

/** Build «greater than» specification from an entity’s property */
fun <T, V : Comparable<V>> KProperty1<T, V>.buildSpecForGreaterThan(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.greaterThan(root.get(name), value)
  }
}

/** Build «greater than or equal to» specification from an entity’s property */
fun <T, V : Comparable<V>> KProperty1<T, V>.buildSpecForGreaterThanOrEqualTo(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.greaterThanOrEqualTo(root.get(name), value)
  }
}
