package uk.gov.justice.digital.hmpps.incidentreporting.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.incidentreporting.config.LocalStackTestcontainer
import uk.gov.justice.digital.hmpps.incidentreporting.constants.InformationSource
import uk.gov.justice.digital.hmpps.incidentreporting.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.incidentreporting.integration.wiremock.ManageUsersMockServer
import uk.gov.justice.digital.hmpps.incidentreporting.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.incidentreporting.service.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.incidentreporting.service.HMPPSMessage
import uk.gov.justice.digital.hmpps.incidentreporting.service.WhatChanged
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SqsIntegrationTestBase : IntegrationTestBase() {

  companion object {
    private val localstackInstance = LocalStackTestcontainer.instance

    @Suppress("unused")
    @JvmStatic
    @DynamicPropertySource
    fun localstackProperties(registry: DynamicPropertyRegistry) {
      localstackInstance?.let { LocalStackTestcontainer.setupProperties(localstackInstance, registry) }
    }

    @JvmField
    val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @JvmField
    val manageUsersMockServer = ManageUsersMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()
      prisonerSearchMockServer.start()
      manageUsersMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      manageUsersMockServer.stop()
      prisonerSearchMockServer.stop()
      hmppsAuthMockServer.stop()
    }
  }

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthorisationHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  protected val testDomainEventQueue by lazy { hmppsQueueService.findByQueueId("test") as HmppsQueue }

  fun HmppsSqsProperties.domaineventsTopicConfig() = topics["domainevents"]
    ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  @BeforeEach
  fun cleanQueue() {
    auditQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(auditQueue.queueUrl).build())
    testDomainEventQueue.sqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(testDomainEventQueue.queueUrl).build(),
    )
    auditQueue.sqsClient.countMessagesOnQueue(auditQueue.queueUrl).get()
    testDomainEventQueue.sqsClient.countMessagesOnQueue(testDomainEventQueue.queueUrl).get()
  }

  fun getNumberOfMessagesCurrentlyOnSubscriptionQueue(): Int? =
    testDomainEventQueue.sqsClient.countMessagesOnQueue(testDomainEventQueue.queueUrl).get()

  fun assertDomainEventSent(eventType: String): HMPPSDomainEvent {
    val sqsClient = testDomainEventQueue.sqsClient
    val request = ReceiveMessageRequest.builder().queueUrl(testDomainEventQueue.queueUrl).build()
    val body = sqsClient.receiveMessage(request).get().messages()[0].body()
    val (message, attributes) = objectMapper.readValue(body, HMPPSMessage::class.java)
    assertThat(attributes.eventType.Value).isEqualTo(eventType)
    val domainEvent = objectMapper.readValue(message, HMPPSDomainEvent::class.java)
    assertThat(domainEvent.eventType).isEqualTo(eventType)

    return domainEvent
  }

  fun assertThatNoDomainEventsWereSent() {
    assertThat(getNumberOfMessagesCurrentlyOnSubscriptionQueue()).isZero
  }

  fun assertThatDomainEventWasSent(
    eventType: String,
    reportReference: String?,
    source: InformationSource = InformationSource.DPS,
    whatChanged: WhatChanged? = null,
  ) {
    getDomainEvents(1).let {
      val event = it[0]
      assertThat(event.eventType).isEqualTo(eventType)
      reportReference?.let {
        assertThat(event.additionalInformation?.reportReference).isEqualTo(reportReference)
      }
      assertThat(event.additionalInformation?.source).isEqualTo(source)
      assertThat(event.additionalInformation?.whatChanged).isEqualTo(whatChanged)
    }
  }

  fun getDomainEvents(messageCount: Int = 1): List<HMPPSDomainEvent> {
    val sqsClient = testDomainEventQueue.sqsClient

    val messages: MutableList<HMPPSDomainEvent> = mutableListOf()
    await untilCallTo {
      messages.addAll(
        sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(testDomainEventQueue.queueUrl).build())
          .get()
          .messages()
          .map { objectMapper.readValue(it.body(), HMPPSMessage::class.java) }
          .map { objectMapper.readValue(it.Message, HMPPSDomainEvent::class.java) },
      )
    } matches { messages.size == messageCount }

    return messages
  }

  protected fun setAuthorisation(
    user: String = "request-user",
    roles: List<String> = emptyList(),
    scopes: List<String> = emptyList(),
  ): (HttpHeaders) -> Unit = jwtAuthorisationHelper.setAuthorisationHeader(
    clientId = "hmpps-incident-reporting-api",
    username = user,
    scope = scopes,
    roles = roles,
  )

  protected fun endpointRequiresAuthorisation(
    endpoint: WebTestClient.RequestHeadersSpec<*>,
    requiredRole: String? = null,
    requiredScope: String? = null,
  ): List<DynamicTest> = buildList {
    val request = endpoint.header("Content-Type", "application/json")

    add(
      DynamicTest.dynamicTest("access forbidden with no authority") {
        request
          .header(HttpHeaders.AUTHORIZATION, null)
          .exchange()
          .expectStatus().isUnauthorized
      },
    )

    add(
      DynamicTest.dynamicTest("access forbidden with no role") {
        request
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      },
    )

    requiredRole?.let {
      add(
        DynamicTest.dynamicTest("access forbidden with wrong role") {
          request
            .headers(setAuthorisation(roles = listOf("ROLE_INCORRECT")))
            .exchange()
            .expectStatus().isForbidden
        },
      )

      requiredScope?.let {
        add(
          DynamicTest.dynamicTest("access forbidden with right role, but wrong scope") {
            request
              .headers(setAuthorisation(roles = listOf("ROLE_$requiredRole"), scopes = listOf("incorrect")))
              .exchange()
              .expectStatus().isForbidden
          },
        )

        add(
          DynamicTest.dynamicTest("access forbidden with right scope, but wrong role") {
            request
              .headers(setAuthorisation(roles = listOf("ROLE_INCORRECT"), scopes = listOf(requiredScope)))
              .exchange()
              .expectStatus().isForbidden
          },
        )
      }
    }
  }

  protected fun Any.toJson(): String = objectMapper.writeValueAsString(this)
}
