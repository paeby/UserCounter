package com.doodle.connectors.csv

import java.io.{BufferedWriter, File, FileWriter}

import com.doodle.containers.PerformanceRecord

class PerformanceWriter {
  val file: File = new File("benchmark/performance.csv")

  private def perfToCsv(perfRecord: PerformanceRecord): String = {
    perfRecord.usedMemory.toString + "," + perfRecord.userFramesProcessed.toString + "," + perfRecord.userFramesBySecond.toString + "," +
    perfRecord.applicationTime.toString + "," + perfRecord.timeInProcessing.toString + "," + perfRecord.percentageInUserProcessing.toString + "," + perfRecord.numberOfRecordsPolled.toString + "," +
    perfRecord.pollRate.toString + "," + perfRecord.countInterval.toString + "\n"
  }

  /**
    * Appends the given performance record inside the performance csv file
    * @param perfRecord PerformanceRecord encapsulating performance metrics
    */
  def writePerformanceRecord(perfRecord: PerformanceRecord) = {
    try {
      val bw: BufferedWriter = new BufferedWriter(new FileWriter(file, true))
      bw.write(perfToCsv(perfRecord))
      bw.close()
    } catch {
      case e: Error => println("Couldn't write performance to CSV " + e.getStackTrace.toString)
    }
  }

}
