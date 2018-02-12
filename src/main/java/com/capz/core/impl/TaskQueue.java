package com.capz.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Executor;

public class TaskQueue {

    static final Logger log = LoggerFactory.getLogger(TaskQueue.class);

    private static class Task {

        private final Runnable runnable;
        private final Executor exec;

        public Task(Runnable runnable, Executor exec) {
            this.runnable = runnable;
            this.exec = exec;
        }
    }


    private final LinkedList<Task> tasks = new LinkedList<>();


    private Executor current;

    private final Runnable runner;

    public TaskQueue() {
        runner = this::run; // 方法引用可看做一个lambda表达式
    }

    private void run() {
        for (; ; ) {
            final Task task;
            synchronized (tasks) {
                task = tasks.poll();
                if (task == null) {
                    current = null;
                    return;
                }
                // 下面的execute方法中可能是不同的executor
                if (task.exec != current) {
                    tasks.addFirst(task);
                    task.exec.execute(runner);
                    current = task.exec;
                    return;
                }
            }
            try {
                task.runnable.run();
            } catch (Throwable t) {
                log.error("Caught unexpected Throwable", t);
            }
        }
    }


    public void execute(Runnable task, Executor executor) {
        synchronized (tasks) {
            tasks.add(new Task(task, executor));
            if (current == null) {
                current = executor;
                executor.execute(runner); // execute会在线程池的某个线程上调用上面的run()方法
            }
        }
    }
}