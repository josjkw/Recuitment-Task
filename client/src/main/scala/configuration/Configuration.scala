package configuration

import com.typesafe.config.Config
import pureconfig.generic.auto._
import pureconfig.{ConfigObjectSource, ConfigSource}

case class Configuration(
  kafka: KafkaConfiguration
)
case class KafkaConfiguration(
  address: String,
  topic: String,
  group: String,
  client: String
)

object Configuration {
  def apply(config: Config): Configuration = {
    val configSource: ConfigObjectSource = ConfigSource.fromConfig(config.getConfig("client"))
    configSource.loadOrThrow[Configuration]
  }
}
