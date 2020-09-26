package com.doodle.stream

import com.doodle.connectors.kafka.{UserConsumer, UserCounterProducer}
import com.doodle.config.UserCounterConfig
import com.doodle.containers.UserRecord
import java.time.Duration
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.collection.JavaConverters._

/**
  * Application starting a Kafka consumer reading User frames (containing a unique user ID and timestamp)
  * and aggregating the number of unique users in the given time window
  */
object UserCounterApp extends App {
  val userConsumer:        UserConsumer        = new UserConsumer(UserCounterConfig.KAFKA_FROM_BEGINNING)
  val userCounterProducer: UserCounterProducer = new UserCounterProducer
  val userCounterState:    UserCounterState    = new UserCounterState(userCounterProducer)

  try {
    while (true) {
      val userRecords: List[ConsumerRecord[Unit, UserRecord]] = try {
        userConsumer.consumer.poll(Duration.ofMillis(UserCounterConfig.KAFKA_POLL_RATE_MS)).asScala.toList
      } catch {
        case _ => {
          List.empty
        }
      }
      userCounterState.processUserRecords(userRecords.map(_.value()))
    }
  } finally {
    userConsumer.consumer.close()
  }
}
