package uk.gov.justice.digital.hmpps.incidentreporting.jpa.specifications

import jakarta.persistence.criteria.Join
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

/** Build «greater than or equal to» specification from an entity’s property */
fun <T, V : Comparable<V>> KProperty1<T, V>.buildSpecForGreaterThanOrEqualTo(value: V): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    criteriaBuilder.greaterThanOrEqualTo(root.get(name), value)
  }
}

/** Build «equal to» specification joining to a related entity (via a collection property) */
fun <T, R, V> KProperty1<T, Collection<R>>.buildSpecForRelatedEntityPropertyEqualTo(
  property: KProperty1<R, V>,
  value: V,
): Specification<T> {
  return Specification { root, _, criteriaBuilder ->
    val relatedEntities: Join<R, T> = root.join(name)
    criteriaBuilder.equal(relatedEntities.get<V>(property.name), value)
  }
}
