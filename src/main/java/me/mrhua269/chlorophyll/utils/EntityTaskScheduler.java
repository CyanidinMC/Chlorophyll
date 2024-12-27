package me.mrhua269.chlorophyll.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityTaskScheduler {
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    public void schedule(Runnable task) {
        taskQueue.add(task);
    }

    public void runTasks() {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }
}
