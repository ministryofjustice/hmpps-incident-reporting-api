package uk.gov.justice.digital.hmpps.incidentreporting.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.incidentreporting.jpa.Report

interface ReportRepositoryCustom {
  fun findAllWithNumericReportReferenceSort(
    specification: Specification<Report>?,
    ascending: Boolean,
    pageable: Pageable,
  ): Page<Report>
}

@Repository
class ReportRepositoryCustomImpl(
  @PersistenceContext
  private val entityManager: EntityManager,
) : ReportRepositoryCustom {

  override fun findAllWithNumericReportReferenceSort(
    specification: Specification<Report>?,
    ascending: Boolean,
    pageable: Pageable,
  ): Page<Report> {
    val cb = entityManager.criteriaBuilder

    // Query for results
    val query = cb.createQuery(Report::class.java)
    val root = query.from(Report::class.java)

    // Apply specification (WHERE clause)
    specification?.let {
      query.where(it.toPredicate(root, query, cb))
    }

    // Since reportReference is always numeric, use PostgreSQL's numeric conversion
    // Using a simple approach: let Hibernate/PostgreSQL handle the cast
    val reportRefAsNumber = cb.function(
      "to_number",
      java.lang.Double::class.java, // Use Double for broader numeric support
      root.get<String>("reportReference"),
      cb.literal("99999999999999999999"), // Format mask for up to 20 digits
    )

    val order = if (ascending) {
      cb.asc(reportRefAsNumber)
    } else {
      cb.desc(reportRefAsNumber)
    }
    query.orderBy(order)

    // Execute query with pagination
    val typedQuery = entityManager.createQuery(query)
    typedQuery.firstResult = pageable.offset.toInt()
    typedQuery.maxResults = pageable.pageSize
    val results = typedQuery.resultList

    // Count query
    val countQuery = cb.createQuery(Long::class.java)
    val countRoot = countQuery.from(Report::class.java)
    countQuery.select(cb.count(countRoot))
    specification?.let {
      countQuery.where(it.toPredicate(countRoot, countQuery, cb))
    }
    val total = entityManager.createQuery(countQuery).singleResult

    return PageImpl(results, pageable, total)
  }
}
