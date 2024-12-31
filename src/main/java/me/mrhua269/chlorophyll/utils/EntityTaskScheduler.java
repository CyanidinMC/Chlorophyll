package me.mrhua269.chlorophyll.utils;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EntityTaskScheduler {
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean destroyed = false;

    public void destroy() {
        this.destroyed = true;
        this.runTasks(); // the final flush
    }

    public boolean schedule(Runnable task) {
        if (this.destroyed) {
            return false;
        }

        taskQueue.add(task);
        return true;
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }

    public void runTasks() {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }
}
