package uk.gov.justice.digital.hmpps.incidentreporting.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.File
import kotlin.also
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.io.readLines
import kotlin.io.walk
import kotlin.jvm.java
import kotlin.sequences.filter
import kotlin.sequences.filterNotNull
import kotlin.sequences.flatMap
import kotlin.sequences.toMutableSet
import kotlin.takeIf
import kotlin.text.contains
import kotlin.text.substringAfter
import kotlin.text.substringBefore

class ResourceSecurityTest : SqsIntegrationTestBase() {
  @Autowired
  private lateinit var context: ApplicationContext

  private val unprotectedDefaultMethods = setOf(
    "GET /v3/api-docs.yaml",
    "GET /swagger-ui.html",
    "GET /v3/api-docs",
    "GET /v3/api-docs/swagger-config",
    "GET /constants/error-codes",
    "GET /constants/information-sources",
    "GET /constants/prisoner-outcomes",
    "GET /constants/prisoner-roles",
    "GET /constants/staff-roles",
    "GET /constants/statuses",
    "GET /constants/types",
    "GET /constants/type-families",
    "GET /constants/user-actions",
    "GET /constants/user-types",
    "PUT /queue-admin/retry-all-dlqs",
    "GET /reports/{reportId}/{reportVariantId}/count",
    "GET /reports/{reportId}/{reportVariantId}",
    "GET /reports/{reportId}/{reportVariantId}/{fieldId}",
    "GET /definitions",
    "GET /definitions/{reportId}",
    "GET /definitions/{reportId}/{variantId}",
    "GET /definitions/{dataProductDefinitionId}/dashboards/{dashboardId}",
    "GET /statements",
    "GET /async",
    "GET /productCollections",
    "GET /reports/{reportId}/dashboards/{dashboardId}",
    "GET /productCollections/{id}",
  )

  @Test
  fun `Ensure all endpoints protected with PreAuthorize`() {
    // need to exclude any that are forbidden in helm configuration
    val exclusions = File("helm_deploy").walk().filter { it.name.equals("values.yaml") }.flatMap { file ->
      file.readLines().map { line ->
        line.takeIf { it.contains("location") }?.substringAfter("location ")?.substringBefore(" {")
      }
    }.filterNotNull().flatMap { path -> listOf("GET", "POST", "PUT", "DELETE").map { "$it $path" } }
      .toMutableSet().also {
        it.addAll(unprotectedDefaultMethods)
      }

    context.getBeansOfType(RequestMappingHandlerMapping::class.java).forEach { (_, mapping) ->
      mapping.handlerMethods.forEach { (mappingInfo, method) ->
        val classAnnotation = method.beanType.getAnnotation(PreAuthorize::class.java)
        val annotation = method.getMethodAnnotation(PreAuthorize::class.java)
        if (classAnnotation == null && annotation == null) {
          mappingInfo.getMappings().forEach {
            assertThat(exclusions.contains(it)).withFailMessage {
              "Found $mappingInfo of type $method with no PreAuthorize annotation"
            }.isTrue()
          }
        }
      }
    }
  }
}

private fun RequestMappingInfo.getMappings() = methodsCondition.methods.map { it.name }.flatMap { method ->
  pathPatternsCondition?.patternValues!!.map { "$method $it" }
}
