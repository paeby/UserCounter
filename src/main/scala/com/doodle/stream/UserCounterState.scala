package com.doodle.stream

import com.doodle.config.UserCounterConfig
import com.doodle.connectors.csv.PerformanceWriter
import com.doodle.connectors.kafka.UserCounterProducer
import com.doodle.containers.{PerformanceRecord, UserCountRecord, UserRecord}

import scala.collection.mutable.HashSet

/**
  * Helper class to count the number of unique users in a time window.
  * It maintains the state of the stream:
  * - The current time window
  * - The user IDs in the current window
  * - The user IDs which are in the next window if the count of unique users in current window is not published to the next topic yet
  * @param userCounterProducer
  */
class UserCounterState(userCounterProducer: UserCounterProducer) {
  type UserId = String

  private val performanceWriter: PerformanceWriter = new PerformanceWriter
  private val countWindowSec:    Long              = UserCounterConfig.COUNT_INTERVAL_MINUTE * 60 // the time window for which we want to count unique users
  private var currentTimeWindow: Long              = 0L

  private var currentWindowUsers: HashSet[UserId] = HashSet.empty
  private var nextWindowUsers:    HashSet[UserId] = HashSet.empty

  // For performance computation purposes
  private var userCounter:      Long = 0
  private val start:            Long = System.currentTimeMillis()
  private var timeInProcessing: Long = 0

  /**
    * Publishes the number of users in the current time window if the last user frame received
    * arrives after the end of the current window + a delay which is set (in order to count late messages)
    * @param latestUser latest UserRecord which is read from Kafka
    * @param userTimeWindow the time window the latestUser is in
    */
  private def tryClosingWindow(latestUser: UserRecord, userTimeWindow: Long) = {
    if (latestUser.ts > currentTimeWindow + countWindowSec + UserCounterConfig.KAFKA_MESSAGE_DELAY_SECOND) {
      // number of unique users in the current time window
      val userCountRecord = UserCountRecord(currentWindowUsers.size, currentTimeWindow)
      // advance the current time window to the next time window
      currentTimeWindow = userTimeWindow
      // next users become current users
      currentWindowUsers = nextWindowUsers.clone()
      // next users is now empty
      nextWindowUsers = HashSet.empty
      // publishes the user count record to Kafka!
      userCounterProducer.writeToKafka(userCountRecord)
    }
  }

  /**
    * Processes the user record read in Kafka:
    * 1. Add it to the current user set if his timestamp is in current time window
    * 2. Add it to the next user set if his timestamp is in the next time window and tries to close current window
    * @param userRecord UserRecord to process
    */
  private def processUserRecord(userRecord: UserRecord) = {
    val userTimeWindow: Long = userRecord.ts - userRecord.ts % countWindowSec

    // If the frame is in the current time window, add the User ID to the current users set
    if (userTimeWindow == currentTimeWindow) {
      currentWindowUsers.add(userRecord.uid)
    }
    // Else if the frame is in the next time window, add it to the next users set and try to close current window
    else if (userTimeWindow > currentTimeWindow) {
      nextWindowUsers.add(userRecord.uid)
      tryClosingWindow(userRecord, userTimeWindow)
    }
    // If the frame is very late it is ignored
  }

  private def printPerformanceInfo(elapsedTime: Long, userRecordCount: Int): Unit = {
    if (userCounter % 100000 == 0) {
      val runtime: Runtime = Runtime.getRuntime
      // Run the garbage collector
      runtime.gc()
      // Calculate the used memory
      val usedMemory: Long = runtime.totalMemory - runtime.freeMemory
      println(s"-------------------------------------------------------------")
      println(s"Used memory is bytes: $usedMemory")
      println(s"Total number of user frames processed: $userCounter")
      val userFramesProcessedBySec: Long = userCounter / (elapsedTime / 1000 + 1)
      println(s"Number of user frames processed by second: $userFramesProcessedBySec")
      println(s"Total appplication time in ms: $elapsedTime")
      println(s"Total time spent in user processing: $timeInProcessing")
      val percentageOfTimeInProc: Long = timeInProcessing / (elapsedTime + 1)
      println(s"Percentage of time spent in user processing: $percentageOfTimeInProc")
      println(s"Number of user records fetched from Kafka by time frame: $userRecordCount")
      println("HashSet size " + currentWindowUsers.size)
      println(s"-------------------------------------------------------------")
      if (userCounter % 1000000 == 0) {
        performanceWriter.writePerformanceRecord(
          PerformanceRecord(
            usedMemory,
            userCounter,
            userFramesProcessedBySec,
            elapsedTime,
            timeInProcessing,
            percentageOfTimeInProc,
            userRecordCount,
            UserCounterConfig.KAFKA_POLL_RATE_MS,
            UserCounterConfig.COUNT_INTERVAL_MINUTE
          )
        )
      }
    }
  }

  def processUserRecords(userRecords: List[UserRecord]): Unit = {
    val userRecordCount: Int = userRecords.size
    userRecords.foreach(userRecord => {
      userCounter += 1
      val startProcessUser = System.currentTimeMillis()
      printPerformanceInfo(startProcessUser - start, userRecordCount)

      processUserRecord(userRecord)

      timeInProcessing += System.currentTimeMillis() - startProcessUser
    })

  }

}
