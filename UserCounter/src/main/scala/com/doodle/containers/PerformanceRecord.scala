package com.doodle.containers

case class PerformanceRecord(usedMemory:                 Long,
                             userFramesProcessed:        Long,
                             userFramesBySecond:         Long,
                             applicationTime:            Long,
                             timeInProcessing:           Long,
                             percentageInUserProcessing: Long,
                             numberOfRecordsPolled:      Long,
                             pollRate:                   Long,
                             countInterval:              Int)
