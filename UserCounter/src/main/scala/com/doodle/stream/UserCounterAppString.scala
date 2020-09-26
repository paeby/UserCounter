package com.doodle.stream

import java.time.Duration

import com.doodle.config.UserCounterConfig
import com.doodle.connectors.kafka.{UserConsumerString}
import org.apache.kafka.clients.consumer.ConsumerRecord
import scala.collection.JavaConverters._

/*
This class is only used to benchmark the deserialization time if reading input as string
 */
object UserCounterAppString extends App {
  private val userConsumer: UserConsumerString = new UserConsumerString(UserCounterConfig.KAFKA_FROM_BEGINNING)

  private val startTime = System.currentTimeMillis()

  private var userCounter = 0

  try {
    while (true) {
      val userRecords: Seq[ConsumerRecord[Unit, String]] = try {
        userConsumer.consumer.poll(Duration.ofMillis(UserCounterConfig.KAFKA_POLL_RATE_MS)).asScala.toList
      } catch {
        case _ => {
          List.empty
        }
      }
      userCounter += userRecords.size

      if (userCounter % 100000 == 0) {
        val now = System.currentTimeMillis()
        println(s"Application time for $userCounter user records in ms: " + (now - startTime))
      }
    }
  } finally {
    userConsumer.consumer.close()
  }

}
