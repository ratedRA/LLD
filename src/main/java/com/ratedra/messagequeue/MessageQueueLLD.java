package com.ratedra.messagequeue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

interface ISubscriber{
    String getId();
    void consume(Message message);

    void subscribe(Topic t);
}

class Message{
    private String id;
    private String payload;

    public Message(String id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }
}

class Topic{
    private String id;
    private String name;
    private List<TopicSubscriber> subscribers;
    private List<Message> messages;

    public synchronized void addMessage(Message message){
        this.messages.add(message);
    }

    public Topic(String id, String name) {
        this.id = id;
        this.name = name;
        subscribers = new ArrayList<>();
        messages = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TopicSubscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<TopicSubscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}

class TopicSubscriber{
    private AtomicInteger offset;
    private ISubscriber subscriber;

    public TopicSubscriber(ISubscriber subscriber) {
        this.offset = new AtomicInteger(0);
        this.subscriber = subscriber;
    }

    public AtomicInteger getOffset() {
        return offset;
    }


    public ISubscriber getSubscriber() {
        return subscriber;
    }
}

class DummySubscriber implements ISubscriber{
    @Override
    public String getId() {
        return "1";
    }

    @Override
    public void consume(Message message) {
        System.out.println("consuming message id: "+ message.getId());
    }

    @Override
    public void subscribe(Topic t) {
        t.getSubscribers().add(new TopicSubscriber(this));
    }
}

class SubscriberWorker implements Runnable{

    private Topic topic;
    private TopicSubscriber topicSubscriber;

    public SubscriberWorker(Topic topic,  TopicSubscriber topicSubscriber) {
        this.topicSubscriber = topicSubscriber;
        this.topic = topic;
    }

    @Override
    public void run() {
        synchronized (topicSubscriber){
            int currOffset = topicSubscriber.getOffset().get();
            if(currOffset > this.topic.getMessages().size()){
                try {
                    topicSubscriber.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
            Message message = this.topic.getMessages().get(currOffset);
            topicSubscriber.getSubscriber().consume(message);

            topicSubscriber.getOffset().compareAndSet(currOffset, currOffset+1);
        }
    }

    public void wakeUp(){
        synchronized (topicSubscriber){
            topicSubscriber.notifyAll();
        }
    }
}

class Broker{

    private Map<String, SubscriberWorker> workers;

    private Broker(){
        workers = new HashMap<>();
    }

    private static Broker INSTANCE = null;

    public static synchronized Broker getINSTANCE(){
        if(INSTANCE == null){
            INSTANCE = new Broker();
        }
        return INSTANCE;
    }

    public void addMessage(Topic topic, Message message){
        topic.addMessage(message);
        new Thread(() -> publish(topic)).start();
    }

    private void publish(Topic t) {
        List<TopicSubscriber> subscribers = t.getSubscribers();
        for(TopicSubscriber topicSubscriber : subscribers){
            ISubscriber subscriber = topicSubscriber.getSubscriber();
            if(!workers.containsKey(subscriber.getId())){
                SubscriberWorker subscriberWorker = new SubscriberWorker(t, topicSubscriber);
                workers.put(subscriber.getId(), subscriberWorker);
                new Thread(subscriberWorker).start();
            } else{
                SubscriberWorker worker = workers.get(subscriber.getId());
                worker.wakeUp();
            }
        }
    }

    public void resetOffset(Topic t, ISubscriber subscriber, int offset){
        for (TopicSubscriber topicSubscriber : t.getSubscribers()){
            if(topicSubscriber.getSubscriber().getId() == subscriber.getId()){
                topicSubscriber.getOffset().set(offset);
                publish(t);
            }
        }
    }
}

public class MessageQueueLLD {
    public static void main(String[] args) {
        Topic t1 = new Topic("1", "firstTopic");
        Topic t2 = new Topic("2", "secondTopic");

        Message m1 = new Message("1", "first Message");
        Message m2 = new Message("2", "second Message");
        Message m3 = new Message("3", "third Message");

        ISubscriber s1 = new DummySubscriber();
        s1.subscribe(t1);
        s1.subscribe(t2);
        ISubscriber s2 = new DummySubscriber();
        s2.subscribe(t1);

        Broker broker = Broker.getINSTANCE();
        broker.addMessage(t1, m1);
        broker.addMessage(t1, m2);
        broker.addMessage(t2, m1);
    }
}
