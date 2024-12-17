package uk.gov.justice.digital.hmpps.incidentreporting.config

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresIfNotRunning() }

  private fun startPostgresIfNotRunning(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      log.warn("Using existing Postgres database")
      return null
    }
    log.info("Creating a Postgres database")
    return PostgreSQLContainer<Nothing>(
      DockerImageName.parse("postgres").withTag("16"),
    ).apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withDatabaseName("incident_reporting")
      withUsername("incident_reporting")
      withPassword("incident_reporting")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)

      start()
    }
  }

  private fun isPostgresRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(5432)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
