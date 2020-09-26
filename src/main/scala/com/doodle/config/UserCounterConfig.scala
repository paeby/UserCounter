package com.doodle.config

import com.typesafe.config.{Config, ConfigFactory, ConfigParseOptions, ConfigSyntax}
import java.io.File
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG

import scala.collection.JavaConverters._

/**
  * Config class reading application.conf in src/main/resources and parsing fields using typesafe config parser
  */
object UserCounterConfig extends {
  private val referenceFile = new File("src/main/resources/application.conf")
  private val referenceConfigOptions: ConfigParseOptions = ConfigParseOptions
    .defaults()
    .setSyntax(ConfigSyntax.CONF)
    .setAllowMissing(false)

  private val config: Config = ConfigFactory.parseFileAnySyntax(referenceFile, referenceConfigOptions)

  val KAFKA_HOSTNAME: String = config.getString("com.doodle.connectors.kafka.hostname")
  val KAFKA_PORT:     String = config.getString("com.doodle.connectors.kafka.port")
  val KAFKA_POLL_RATE_MS = config.getLong("com.doodle.connectors.kafka.poll_rate_ms")

  // "auto.offset.reset" set to "earliest" in order to read a topic from the beginning if subscribing to a new group
  def KAFKA_CONSUMER_CONFIG(group: String) =
    Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG -> s"$KAFKA_HOSTNAME:$KAFKA_PORT", "group.id" -> group, "auto.offset.reset" -> "earliest").asJava

  val KAFKA_PRODUCER_CONFIG =
    Map[String, AnyRef](BOOTSTRAP_SERVERS_CONFIG -> s"$KAFKA_HOSTNAME:$KAFKA_PORT").asJava

  val USER_RECORD_TOPIC:  String = config.getString("com.doodle.connectors.kafka.user_record_topic")
  val USER_COUNTER_TOPIC: String = config.getString("com.doodle.connectors.kafka.user_counter_topic")

  val COUNT_INTERVAL_MINUTE:      Int     = config.getInt("com.doodle.stream.count_interval_in_minute")
  val KAFKA_MESSAGE_DELAY_SECOND: Int     = config.getInt("com.doodle.stream.kafka_message_delay_in_second")
  val KAFKA_FROM_BEGINNING:       Boolean = config.getBoolean("com.doodle.stream.from_beginning")

}
