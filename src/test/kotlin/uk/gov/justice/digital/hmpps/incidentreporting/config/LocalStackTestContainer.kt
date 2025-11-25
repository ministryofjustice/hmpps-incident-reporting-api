package uk.gov.justice.digital.hmpps.incidentreporting.config

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName

object LocalStackTestContainer : TestContainer<LocalStackContainer>("localstack", 4566) {
  override fun start(): LocalStackContainer {
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    log.info("Starting localstack")
    return LocalStackContainer(
      DockerImageName.parse("localstack/localstack").withTag("4"),
    ).apply {
      withServices("sqs", "sns")
      withEnv("DEFAULT_REGION", "eu-west-2")
      start()
      followOutput(logConsumer)
    }
  }

  override fun setupProperties(instance: LocalStackContainer, registry: DynamicPropertyRegistry) {
    val localstackUrl = instance.endpoint
    registry.add("hmpps.sqs.localstackUrl") { localstackUrl }
    registry.add("hmpps.sqs.region", instance::getRegion)
  }
}
