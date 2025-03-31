package uk.gov.justice.digital.hmpps.incidentreporting.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import javax.sql.DataSource

const val PRIMARY_DATASOURCE_PREFIX: String = "spring.datasource"
const val REPLICA_DATASOURCE_PREFIX: String = "spring.replica.datasource"

@Configuration
class PersistenceConfiguration(
  private val environment: Environment,
) {

  @Bean
  @Primary
  @ConfigurationProperties(prefix = PRIMARY_DATASOURCE_PREFIX)
  fun primaryDataSource(): DataSource {
    return buildDataSource(
      "PrimaryHikariPool",
      PRIMARY_DATASOURCE_PREFIX,
      false,
    )
  }

  @Bean("replicaDataSource")
  @ConditionalOnProperty("spring.replica.datasource.url")
  @ConfigurationProperties(prefix = REPLICA_DATASOURCE_PREFIX)
  fun replicaDataSource(): DataSource {
    return buildDataSource(
      "ReplicaHikariPool",
      REPLICA_DATASOURCE_PREFIX,
      true,
    )
  }

  @Bean("replicaDataSource")
  @ConditionalOnProperty("!spring.replica.datasource.url")
  fun replicaDataSourceMock(dataSource: DataSource): DataSource {
    return dataSource
  }

  private fun buildDataSource(
    poolName: String,
    dataSourcePrefix: String,
    readonly: Boolean,
  ): DataSource {
    val url: String? = environment.getProperty(String.format("%s.url", dataSourcePrefix))
    val hikariConfig = HikariConfig()
    hikariConfig.poolName = poolName

    hikariConfig.jdbcUrl = url
    hikariConfig.username = environment.getProperty(String.format("%s.username", dataSourcePrefix))
    hikariConfig.password = environment.getProperty(String.format("%s.password", dataSourcePrefix))

    val maxPoolSize: String? = environment.getProperty(String.format("%s.hikari.maximum-pool-size", dataSourcePrefix))
    maxPoolSize?.let {
      hikariConfig.maximumPoolSize = it.toInt()
    }
    val maxLifetime: String? = environment.getProperty(String.format("%s.hikari.max-lifetime", dataSourcePrefix))
    maxLifetime?.let {
      hikariConfig.maxLifetime = it.toLong()
    }
    val connectionTimeout: String? = environment.getProperty(
      String.format("%s.hikari.connectionTimeout", dataSourcePrefix),
    )
    connectionTimeout?.let {
      hikariConfig.connectionTimeout = it.toLong()
    }
    val validationTimeout: String? = environment.getProperty(
      String.format("%s.hikari.validationTimeout", dataSourcePrefix),
    )
    validationTimeout?.let {
      hikariConfig.validationTimeout = it.toLong()
    }

    hikariConfig.isReadOnly = readonly
    return HikariDataSource(hikariConfig)
  }
}
