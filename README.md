# Doodle Data Engineer Challenge

This project is a proposed prototype for the Data Engineer Challenge sent by Doodle.
You can find the requirements of the project here [The challenge (Data Engineer)](https://github.com/tamediadigital/hiring-challenges/tree/master/data-engineer-challenge).

## Building and running the project

Please find below the prerequisites and the steps to run the `Kafka User Counter` application.

### Prerequisites
* It is a `Scala` project, so you need to install `sbt` to compile the project. Please refer to this link [SBT installation](https://docs.scala-lang.org/getting-started/sbt-track/getting-started-with-scala-and-sbt-on-the-command-line.html).
* `Kafka` service should be running locally on port `9092`
* `Zookeeper` service should be running locally on port `2181`

Hostname and port of `Kafka` and `Zookeeper` can be configured in `src/main/resources/application.conf`)

### Building the project
* Inside a terminal, clone the project `git clone https://github.com/paeby/UserCounter.git` and go inside `UserCounter` folder.
* At the root of the project, run `sbt compile`. All dependencies should be successfully downloaded.

### Starting the application
* You can run the script `create_kafka_topics.sh` which creates one topic `userframes` that will be read by the application, and one topic `usercounter` where the application will publish the count of unique `User IDs` per time frame. The script also publishes 1000 `User frames` (from `sampledata.json` file) to `userframes` topic. You can modify directly the script if ports / hostnames of `Kafka` and `Zookeeper` are different in your environment.
* Then, you can run `sbt run` and the application will start. 
* The count of unique `User IDs` per minute is published to `usercounter` topic, so you can look at the results with `kafka-console-consumer --bootstrap-server [KAFKA_BROKER_HOST]:[KAFKA_BROKER_PORT] --topic usercounter`. 
    * It publishes the JSON message `{"uniqueUsers":1,"ts":1468245240}` corresponding to the number of unique users in the time window beginning at `ts`.
    * Performance metrics are also output to the console (every 10k user frames processed)

### Application configuration
* The application configuration can be modified in `src/main/resources/application.conf`:

```
com.doodle {
    connectors.kafka {
        hostname = "localhost"                    // Kafka broker hostname
        port = "9092"                             // Kafka broker port
        user_record_topic = "userframes"          // Input Topic containing user frames 
        user_counter_topic = "usercounter"        // Output Topic to which count of unique User IDs per time frame are published
        poll_rate_ms = 100                        // Rate in ms at which the application poll messages from user_record_topic
    }
    stream {
        count_interval_in_minute = 1              // Time frame for which we want to count unique users. Default is one minute
        kafka_message_delay_in_second = 5         // User frames arriving after this delay are still counted in the time frame
        from_beginning = true                     // Reads Kafka messages from the beginning of the stream if set to true, otherwise starts from latest commited offset
    }
}
```

## Application functionalities
* The `UserCounter` application instantiates a `Kafka Consumer` which subscribes to `userframes` Kafka topic containing `User frames` in JSON format. 
* It polls messages from `Kafka` at a given interval (by default 100ms) and processes each `User frame` in order to count the number of unique `User IDs` in a given time frame (by default 1 minute). To do so, it deserializes the `User frame` and gets the `ts` and `uid` fields.
* When the count of unique `User IDs` is collected for the current time window, the application publishes to `usercounter` Kafka topic the following message in JSON format: 
    * `{"uniqueUsers":34981,"ts":1468245240}` : `uniqueUsers` being the count of unique `User IDs` and `ts` being the timestamp corresponding to the beginning of the time frame
* The `User frames` can be processed from the beginning of the topic or from the latest committed offset.
* Edge cases the application supports:
    * If a `User frame` arrives after given delay (by default 5 seconds), the `User frame` is still counted in its time window.
    * If a `User frame` arrives after the given delay, it is ignored.
    * If a `User frame` doesn't contain the right JSON format, it is ignored.

## Design choices: why Scala?
I chose to use `Scala` for this project first because I really enjoy this functional programming language but also because:
* there is an existing kafka-client package which is intuitive to use, and Kafka is actually written in Scala :) 
* it is a statically-typed language, so a large type of errors are already caught at compile-time
* you can control the resources you give to your application through the JVM parameters
* the garbage collection is cheap and efficient

## Project structure
It is important to have a good project structure in order to decouple the data pipeline connectors, the data structure and the application logic.
The project is composed of the following packages:

### com.doodle.config
This package contains the `UserCounterConfig` object which reads the `src/main/resources/application.conf` configuration file in Typesafe format. It provides the `config` which can be accessed by all other classes.

### com.doodle.connectors
This package contains the data pipeline connectors. It is composed of the `kafka` package encapsulating the Kafka consumer and producer. It has also a `csv` connector used to write performance metrics to a CSV file.
Other connectors could be added for example to consume other topics and we could create a generic `Consumer` and `Producer`abstract class.

### com.doodle.containers
Case classes encapsulating the data structures used in the project are in the `containers` package. It contains the input/output topic format and the performance data we write to the CSV. Scala `case classes` are very useful to model immutable data.

### com.doodle.stream
The `User Counter Application` resides inside the `stream` package. It starts the kafka consumer and finishes only if we interrupt the program.
It also instantiates a `UserCounterState` which helps counting the number of unique `User IDs` in the current time window. Please find below the details of the counting algorithm:

#### User Counter Algorithm
We know that `User frames` arrive 99.9% of the time within a delay of 5 seconds. We want therefore to count the number of unique users within the current time window and include `User frames` arriving a bit late before publishing to the output topic the number of unique `User IDs`.
To do so, we keep the following state variables:
* `currentTimeWindow`: the timestamp of the beginning of the current time window. It is the `currentTimestamp - currentTimestamp % [WINDOW TIME INTERVAL]`. By default `[WINDOW TIME INTERVAL]` is set to 1 minute.
* `currentWindowUsers`: a `HashSet<UserId>` containing the `User IDs` with a `ts` in the `currentTimeWindow`
* `nextWindowUsers`: a `HashSet<UserId>` containing the `User IDs` arriving in the next time window, before the current time window is closed as a delay is tolerated.
We choose to use `HashSet` as adding an element is done in O(1) and also getting the set size.

The pseudocode of processing the `User frames`:
```
for each userRecord:
    val userTimeWindow =  userRecord.ts - userRecord.ts % [WINDOW TIME INTERVAL]
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
```

The method `tryClosingWindow` will close the current window if `latestUser.ts > currentTimeWindow + [WINDOW TIME INTERVAL] + [MESSAGE DELAY]`:

```    
tryClosingWindow:  
    // number of unique users in the current time window
    val userCountRecord = UserCountRecord(currentWindowUsers.size, currentTimeWindow)
    // advance the current time window to the next time window
    currentTimeWindow = userTimeWindow
    // next users become current users
    currentWindowUsers = nextWindowUsers
    // next users is now empty
    nextWindowUsers = HashSet.empty
    // publishes the user count record to Kafka!
    userCounterProducer.writeToKafka(userCountRecord)
```
In fact, we want to output the count of unique users as soon as possible, with a latency of 5 seconds to count 99.9% of the `User frames`. We can increase this latency to achieve higher precision.

In order to test the logic, I put the following records in the input topic which counts records for every minute:
```
{"ts":1,"uid":"1"}
{"ts":2,"uid":"2"}
{"ts":60,"uid":"3"}
{"ts":65,"uid":"4"}
                   ====> publishes {"uniqueUsers":2,"ts":0}
{"ts":70,"uid":"5"}
{"ts":71,"uid":"5"}
{"ts":72,"uid":"6"} 
{"ts":170,"uid":"10"}
                   ====> publishes {"uniqueUsers":4,"ts":60}
{"ts":165,"uid":"9"}
{"ts":160,"uid":"8"}
{"ts":180,"uid":"11"}
{"ts":1,"uid":"30"}
{"ts":300,"uid":"31"}
                    ====> publishes{"uniqueUsers":3,"ts":120}
{"ts":400,"uid":"31"}
                    ====> publishes {"uniqueUsers":1,"ts":360}

```
*Note: In order to improve the testing we could add a test class and modify the `processUserRecord` function to return a `UserCountRecord` and verify that for an array of `UserRecord` the expected `UserCountRecord` are returned.*

## Performance tests
### Testing environment
#### Hardware
Tests have been run on a MacBook Pro 2.7 GHz Intel Core i7 with 16 GB or RAM.

#### Data
The 1 million records provided in http://tx.tamedia.ch.s3.amazonaws.com/challenge/data/stream.jsonl.gz have been published to a `userframesperf` topic.
At each run of the application, the messages in the topics were consumed from the beginning (subscribing to the topic with a new group id and therefore reading from offset 0).

#### Metrics
* `User frames` processed by second
* Total total application time in ms
* The time spent in processing the `User frames` (User Counter Algorithm described above)
* The number of `User frames` polled by the kafka consumer in the poll interval (100ms by default)
* The application memory usage

The metrics are saved to a CSV file in `benchmark/performance.csv` for each run, and also to stdout every 10k `User frames` processed.
Application variables are used to compute the metrics and the application `Runtime` to get the memory usage.

### Benchmarks
All run metrics are saved in `benchmark/performance.csv`.

#### JSON Serialization cost
In order to compute the cost of the deserialization, we can run the application with a Kafka consumer deserializing the values as string.
There is a `UserCounterAppString` application which can be started with `sbt "runMain com.doodle.stream.UserCounterAppString"`. It outputs to stdout the application running time to process the 1 million `User frames`.
It takes 5 seconds to process the 1 million records, so it processes **200k records by second**.
Now, if we run the `UserCounterApp` on the same data with the same configurations, we obtain the following performance output:

|                     |                    |                   |                    |                            | 
|---------------------|--------------------|-------------------|--------------------|----------------------------| 
| userFramesProcessed | userFramesBySecond | applicationTimeMs | timeInProcessingMs | percentageInUserProcessing | 
| 1000000             | 45454              | 21713             | 2173               | 0                          | 

The application deserializing the JSON data takes now 21 seconds to process the 1 million records, so **45k records by second**, which is **4.4 times longer**.
We also see that the time spent in processing the `User frames` can be neglected as  it represents only 0.09% of the processing.

**Alternatives to JSON**: 
- `Avro` seems to be widely use for (de)serializing data to/from Kafka. It supports direct mapping to JSON and has a compact format. However, from this article https://labs.criteo.com/2017/05/serialization/ comparing serializers, we can see that `Thrift` and `Protobuf` achieve better performances.
- `Custom serializer`: we could also (de)serialize directly Scala case classes with custom serializers

We should test and benchmark different serializers before going with a specific one as we can observe it is the bottleneck of the application.

#### Counting per minute / hour / day / month / year
We run the application counting the number of unique `User IDs` by minute/hour/day/month/year.
The number of unique `User IDs` stored in the HashMap is increasing when the time frame is larger, so we keep more data in memory.
It is confirmed if we look at the memory usage which increases from 50mb for a time frame of 1min to 55mb for a time frame of 1year.
We can also observe that the number of `User Frames` processed by second increases as we don't close time windows as frequently when aggregating over a longer period.

|                   |                    |                   |                    |                            |               | 
|-------------------|--------------------|-------------------|--------------------|----------------------------|---------------| 
| usedMemoryInBytes | userFramesBySecond | applicationTimeMs | timeInProcessingMs | percentageInUserProcessing | countInterval | 
| 50139640          | 40000              | 24702             | 2133               | 0                          | 1 (1min)      | 
| 54827032          | 45454              | 21266             | 2345               | 0                          | 60 (1h)       | 
| 54801872          | 47619              | 20661             | 2180               | 0                          | 1440 (1 day)  | 
| 54867664          | 47619              | 20965             | 2244               | 0                          | 43200 (1 mth) | 
| 54841912          | 47619              | 20611             | 2246               | 0                          | 525600 (1 yr) | 

#### Kafka consumer poll rate
In this benchmark we compare the frequency at which messages are consumed from Kafka. We can see that increasing the poll time we can achieve better performances (higher user frames per second.)

|                   |                    |                   |                    |                            |                            |               |      | 
|-------------------|--------------------|-------------------|--------------------|----------------------------|----------------------------|---------------|------| 
| usedMemoryInBytes | userFramesBySecond | applicationTimeMs | timeInProcessingMs | percentageInUserProcessing | numberOfRecordsPolledKafka | kafkaPollRate |      | 
| 50023072          | 41666              | 23799             | 2628               | 0                          | 435                        | 1             |      | 
| 49967280          | 45454              | 21253             | 2245               | 0                          | 435                        | 100           |      | 
| 49927776          | 47619              | 20810             | 2081               | 0                          | 435                        | 1000          |      | 
| 49991296          | 45454              | 21862             | 2271               | 0                          | 435                        | 10000         |      | 

*Note about the benchmarks:* we should run each test at least 10 times to get more accurate performance metrics. Also, we should generate more data and test how the application scales with a higher number of unique `User IDs` per time frame.

### Scaling
1. **Parallelize consumers / producers**: we could have several instances of consumers and producers. For example, if we increase the number of partitions of each Kafka topic, we can assign each consumer to a specific partition. Then when we write to the counter topic, we could also randomly select a producer which writes to a specific partition.
2. **Increase number of topics**: parallelization can also be achieved by increasing the number of kafka topics. We then need to customize our consumers/producers to read/write to random or specific topics.
3. **Distributed setup**: if we want to scale the application over a cluster of machines, we might want to use some Big Data frameworks which already handle the distribution of Streaming applications. We could analyze the different frameworks, do advanced benchmark and move with the appropriate solution. There is for example Kafka Streams, Spark Streaming, Storm, Samza, ... There are some interesting articles comparing those frameworks (e.g. https://medium.com/@chandanbaranwal/spark-streaming-vs-flink-vs-storm-vs-kafka-streams-vs-samza-choose-your-stream-processing-91ea3f04675b). It is also important to consider the learning curve and the current tech stack.
4. **Deployment on premise or on the cloud?**: based on the use case, we might want to deploy the application in a cloud environment. The advantage of deploying on the cloud is that there is no upfront investment so we can easily scale the resources as needed. The deployment is also easier to manage (no need to handle cluster of servers, redundancy and disaster recovery usually handled by DevOps). We might want to investigate solutions offered by AWS, Azure and Google Cloud and choose the one offering the best features and services for our use case.

### Counting for different time frames 
We can have one topic dedicated for a specific time frame. We could have a "minute" topic, an "hour" topic, etc. and the application should read/write to the appropriate topic. For example, to do aggregation over one hour we could consume the output of the "minute" counter.

### Coping with failure
When the application is processing the messages from Kafka, it commits to a specific partition that the message was consumed. If the application crashes, we can specify to the consumer to read the messages from the last commited offset.
However, the `User IDs` saved in the current `HashSet` are lost and not counted which might lead to inconsistent data. We could for example have in a shutdown hook the `User IDs` saved to a file (HDFS file system is a good option if we run in a Hadoop environment) which can be read when the application is re-started.
