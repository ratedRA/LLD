package com.ratedra.thread;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  queue - in which we could add new messages and also at last remove from it
 we'll be doing through multiple threads.
 we'll queue around 1 million of items and also removing all of them
 expecting - some kind race condition (because resource. i.e - queue)
 try to resolve the race condition using some kind of locks*/
public class ConcurrentQueueImpl {
    public static void main(String[] args) {
        int numOfItem = 1000000;
        Queue<Integer> queue = new LinkedList<>();
        int threadPoolSize = 10;
        Lock lock = new ReentrantLock();

        // two List<thread>  -> producer, consumer
        Thread[] producerThread = new Thread[threadPoolSize];
        // 100 items -> 100/10 -> 10 items
        for(int i=0; i<threadPoolSize; i++){
            producerThread[i] = new Thread(
                    new Producer(numOfItem/threadPoolSize,
                            queue, lock));
            producerThread[i].start();
        }

        Arrays.stream(producerThread).forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println(queue.size());

        Thread[] consumerThread = new Thread[threadPoolSize];
        for(int i=0; i<threadPoolSize; i++){
            consumerThread[i] = new Thread(
                    new Consumer(numOfItem/threadPoolSize, queue, lock));
            consumerThread[i].start();
        }

        Arrays.stream(consumerThread).forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println(queue.size());


    }

    private static class Producer implements Runnable{
        int numOfItems;
        Queue<Integer> queue;
        Lock lock;

        public Producer(int numOfItems, Queue<Integer> queue, Lock lock) {
            this.numOfItems = numOfItems;
            this.queue = queue;
            this.lock=lock;
        }

        @Override
        public void run() {
            for(int i=0; i<numOfItems; i++){
                try {
                    lock.lock();
                    queue.add(i);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    private static class Consumer implements Runnable{
        int numOfItems;
        Queue<Integer> queue;
        Lock lock;

        public Consumer(int numOfItems, Queue<Integer> queue, Lock lock) {
            this.numOfItems = numOfItems;
            this.queue = queue;
            this.lock = lock;
        }

        @Override
        public void run() {
            for(int i=0; i<numOfItems; i++){
                try {
                    lock.lock();
                    queue.poll();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

}
