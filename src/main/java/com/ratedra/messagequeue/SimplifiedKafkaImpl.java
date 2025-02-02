package com.ratedra.messagequeue;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Topic - topic has partitions
 * Partition - partition has messages
 * ConsumerGroup - ConsumerGroup has consumers
 * Consumer - Consumer has partition associated with it and offset
 * Topics should have list of consumerGroups associated with
 * consumers in a CG should be less than or equal to partitions in a topic
 */
public class SimplifiedKafkaImpl {
    public static void main(String[] args) {

    }

    @Getter
    @Setter
    class Topic{
        private static final int DEFAULT_PARTITIONS = 1;

        private String name;
        private List<Partition> partitions;
        private List<ConsumerGroup> consumerGroups;
        private int totalPartitionsCount = DEFAULT_PARTITIONS;

        public Topic(String name, int totalPartitionsCount) {
            this.totalPartitionsCount = totalPartitionsCount;
            this.name = name;
            this.partitions = new ArrayList<>();
            this.consumerGroups = new ArrayList<>();
            for(int partition=0; partition<totalPartitionsCount; partition++){
                Partition createdPartition = new Partition(name + "-" + partition);
                partitions.add(createdPartition);
            }
        }

        public List<Partition> getPartitions() {
            return partitions;
        }

        public void setPartitions(List<Partition> partitions) {
            this.partitions = partitions;
        }
    }

    @Getter
    @Setter
    class Partition{
        private String name;
        private List<Message> messages;

        public Partition(String name) {
            this.name = name;
            this.messages = new ArrayList<>();
        }
    }

    @Getter
    @Setter
    class Message{
        private int id;
        private String data;

        public Message(int id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "id=" + id +
                    ", data='" + data + '\'' +
                    '}';
        }
    }

    @Getter
    @Setter
    class ConsumerGroup{
        Map<String, Consumer> consumerByPartition = new HashMap<>();
        private String name;
        private Topic topic;
        private List<Consumer> consumers;

        public ConsumerGroup(String name, Topic topic) {
            this.name = name;
            this.topic = topic;
            consumers = new ArrayList<>();
            initConsumers();
        }

        private void initConsumers(){
            List<Partition> partitions = this.topic.partitions;
            for(Partition partition : partitions){
                consumers.add(new Consumer(partition.name, this.name, partition, 0));
            }
        }
    }

    @Getter
    @Setter
    class Consumer{
        private String id;
        private String groupName;
        private Partition partition;
        private int offset;

        public Consumer(String id, String groupName, Partition partition, int offset) {
            this.id = "consumer-"+id;
            this.groupName = groupName;
            this.partition = partition;
            this.offset = offset;
        }

        public void consume(){
            List<Message> messages = partition.messages;
            List<Message> fromOffset = messages.subList(offset, messages.size());
            System.out.println(String.format("Consumer: %s started consuming from offset: %s for partition: %s", this.id, this.offset, this.partition.name));
            for (Message message : fromOffset){
                System.out.println("read message: " + message.toString());
                this.offset+=1;
            }
        }
    }

    class KafkaBroker{
        Map<String, Topic> topics = new HashMap<>();
        Lock topicCreationLock = new ReentrantLock();
        ExecutorService consumerGroupExecutor = Executors.newFixedThreadPool(2);
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(2);

        void addTopic(String name, int partitions){
            try {
                topicCreationLock.lock();
                if (topics.containsKey(name)) {
                    throw new RuntimeException("topic with name already exists, try giving some other name")
                }
                Topic topic = new Topic(name, partitions);
                topics.put(name, topic);
            } catch (Exception exception){
                topicCreationLock.unlock();
                throw exception;
            } finally {
                topicCreationLock.unlock();
            }
        }

        void addConsumerGroup(String tName, String gName){
            Topic topic = topics.get(tName);
            ConsumerGroup consumerGroup = new ConsumerGroup(gName, topic);
            topic.consumerGroups.add(consumerGroup);
        }

        void publishMessage(String topicName, Message message){
            // get all partitions from the topicName
            // get the partition from the message

            Topic topic = topics.get(topicName);
            List<Partition> partitions = topic.partitions;
            int totalPartitionsCount = topic.totalPartitionsCount;

            int partitionIdx = message.id % totalPartitionsCount;
            partitions.get(partitionIdx).messages.add(message);
        }

        void consumeMessage(String topicName){
            // get all CGs for the topic
            // get all consumers and read from the assigned partition

            Topic topic = topics.get(topicName);
            List<ConsumerGroup> consumerGroups = topic.consumerGroups;

            consumerGroups.forEach(consumerGroup -> {
                CompletableFuture.runAsync(() ->{
                    consumerGroup.consumers.forEach( consumer -> {
                        CompletableFuture.runAsync(consumer::consume, consumerExecutor);
                    });
                }, consumerGroupExecutor);
            });
        }
    }
}
