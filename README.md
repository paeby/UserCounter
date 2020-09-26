# Doodle Data Engineer Challenge

This project is a proposed prototype for the Data Engineer Challenge sent by Doodle.
You can find the requirements of the project here [The challenge (Data Engineer)](https://github.com/tamediadigital/hiring-challenges/tree/master/data-engineer-challenge).

## Building and running the project

Please find below the prerequisites, and the steps to run the `Kafka User Counter` application.

### Prerequisites
* It is a `Scala` project, so you need to install `sbt` to compile the project. Please refer to this link [SBT installation](https://docs.scala-lang.org/getting-started/sbt-track/getting-started-with-scala-and-sbt-on-the-command-line.html).
* You need to have `Kafka` service running locally on port `9092` (this can be configured in the `application.conf`)
* You need to have `Zookeeper` service running locally on port `2181`

### Building the project
* Clone the project ``
* At the root of the project, simply run `sbt compile`. All dependencies should be successfully downloaded.

### Starting the application
* You can run the script `create_kafka_topics.sh` which will create one topic `userframes` that will be read from the application, one topic `usercounter` where the unique user count per time frame will be published. The script also publishes 1000 user frames (from `sampledata.json` file) to `userframes` topic. You can modify directly the script if ports / hostnames of `Kafka` and `Zookeeper` are different in your environment.
* Then, you can run `sbt run` and the application will start. 
* The count of unique user IDs per minute is published to `usercounter` topic, so you can simply look at the results with `kafka-console-consumer --bootstrap-server [KAFKA_BROKER_HOST]:[KAFKA_BROKER_PORT] --topic usercounter`. It published the JSON message `{"uniqueUsers":1,"ts":1468245240}` corresponding to the number of unique users in the time window beginning at `ts`.

### Application configuration
* The application configuration can be modified in `src/main/resources/application.conf`:

```
com.doodle {
    connectors.kafka {
        hostname = "localhost"                    // Kafka broker hostname
        port = "9092"                             // Kafka broker port
        user_record_topic = "userframes"          // Input Topic containing user frames 
        user_counter_topic = "usercounter"        // Output Topic to which unique user count per time frame are published
        poll_rate_ms = 100                        // Rate in ms at which the application poll messages from user_record_topic
    }
    stream {
        count_interval_in_minute = 1              // Time frame for which we want to count unique users. Default is one minute
        kafka_message_delay_in_second = 5         // User frames arriving after this delay are still counted in the time frame
        from_beginning = true                     // Reads Kafka messages from the beginning of the stream if set to true, otherwise starts from latest offset
    }
}
```

- data structure choices
- project structure, how to improve connectors, build on the top of the application
- performance bottlenecks
- costs of serialization: why json 
-  sbt "runMain com.doodle.stream.UserCounterAppString" 
Basic solution

Install Kafka
Create a topic
Use the Kafka producer from kafka itself to send our test data to your topic
Create a small app that reads this data from kafka and prints it to stdout
Find a suitable data structure for counting and implement a simple counting mechanism, output the results to stdout
Advanced solution

Benchmark
Output to a new Kafka Topic instead of stdout
Try to measure performance and optimize
Write about how you could scale
Only now think about the edge cases, options and other things
Bonus questions / challenges:

How can you scale it to improve throughput?
You may want count things for different time frames but only do json parsing once.
Explain how you would cope with failure if the app crashes mid day / mid year.
When creating e.g. per minute statistics, how do you handle frames that arrive late or frames with a random timestamp (e.g. hit by a bitflip), describe a strategy?
Make it accept data also from std-in (instead of Kafka) for rapid prototyping. (this might be helpful to have for testing anyways)
Measure also the performance impact / overhead of the json parser