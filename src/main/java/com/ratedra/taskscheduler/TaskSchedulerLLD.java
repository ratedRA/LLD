package com.ratedra.taskscheduler;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The task scheduler should have a fixed number of worker threads that are responsible for executing the tasks.
 * Each task should have an associated priority value (integer) indicating its priority. The lower the value, the higher the priority.
 * Tasks with higher priority values should be executed before the tasks with lower priority values.
 * The task scheduler should ensure that only one task is executed at a time by a worker thread.
 * The task scheduler should provide a method to add tasks with their priority to the scheduler.
 * The task scheduler should execute the tasks concurrently using multiple worker threads.
 * The task scheduler should print the execution order of the tasks as they are completed.
 */
public class TaskSchedulerLLD {
    public static void main(String[] args) {
        TaskScheduler scheduler = new TaskScheduler();

        scheduler.addTask(new Task("Task A", 2));
        scheduler.addTask(new Task("Task B", 1));
        scheduler.addTask(new Task("Task C", 3));
        scheduler.addTask(new Task("Task D", 2));
        scheduler.addTask(new Task("Task E", 1));

        scheduler.start();
    }
}

class Task{
    String name;
    Integer priority;

    public Task(String name, Integer priority) {
        this.name = name;
        this.priority = priority;
    }
}

class TaskScheduler{
    private static final int THREAD_POOL_SIZE = 5;
    private Lock lock;
    private Queue<Task> taskQueue;
    private ExecutorService executorService;

    public TaskScheduler() {
        this.lock = new ReentrantLock();
        taskQueue = new PriorityQueue<>((task1, task2) -> task1.priority.compareTo(task2.priority));
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void addTask(Task task){
        lock.lock();
        try{
            taskQueue.offer(task);
        } finally {
            lock.unlock();
        }
    }

    public void start(){
        while(!taskQueue.isEmpty()){
            lock.lock();
            try {
                Task task = taskQueue.poll();
                Callable<Void> taskJob = () -> {
                    System.out.println("executing -> " + task.name);
                    return Void.TYPE.newInstance();
                };
                executorService.submit(taskJob);
            } finally {
                lock.unlock();
            }
        }
        executorService.shutdown();
    }
}
