kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic userframes
kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic usercounter
kafka-console-producer --broker-list localhost:9092 --topic userframes < sampledata.json
