package thread_pool;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MyThreadPoolExecutor {
    private final int coreSize; // 核心线程数量
    private final int maxSize; // 线程池最多线程数量
    private final long keepAliveTime; // 非核心线程的最长等待时间，之后销毁
    private final TimeUnit unit; // keepAliveTime的时间单位
    private final BlockingQueue<Runnable> taskQueue; // 任务队列
    private final MyRejectedExecutionHandler rejectedExecutionHandler; // 拒绝策略

    private final HashSet<Worker> workers = new HashSet<>(); // 工作线程集合
    private final Object lock = new Object(); // 锁

    public MyThreadPoolExecutor(int coreSize, int maxSize, long keepAliveTime,
            TimeUnit unit, int queueCapacity, MyRejectedExecutionHandler rejectedExecutionHandler) {
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.rejectedExecutionHandler = rejectedExecutionHandler;

        for (int i = 0; i < coreSize; i++) {
            addWorker(true);
        }
    }

    public void execute(Runnable task) {
        boolean success = taskQueue.offer(task);
        if (!success) { // 任务队列满
            synchronized (lock) {
                if (workers.size() < maxSize) { // 且线程池没有达到最大负荷
                    addWorker(false); // 创建一个非核心线程
                    taskQueue.offer(task); // 再次尝试入队
                    return;
                }
            }

            // 线程池已达最大负荷，走拒绝策略
            rejectedExecutionHandler.rejectedExecution(task, this);
        }
    }

    private void addWorker(boolean isCore) {
        Worker worker;
        if (isCore) {
            worker = new Worker("core-worker-" + workers.size(), true);
        } else {
            worker = new Worker("non-core-worker-" + workers.size(), false);
        }
        synchronized (lock) {
            workers.add(worker);
        }

        worker.start();
    }


    private class Worker extends Thread {

        private final boolean core;

        public Worker(String name, boolean core) {
            super(name);
            this.core = core;
        }

        @Override
        public void run() {
            try {
                while (true) {
                   Runnable task;
                   if (core) {
                       // 核心线程，一直阻塞地等待任务
                       task = taskQueue.take();
                   } else {
                       // 非核心线程
                       // 最多等待keepAliveTime，然后退出
                       task = taskQueue.poll(keepAliveTime, unit);
                       if (task == null) {
                           synchronized (lock) {
                               workers.remove(this);
                           }

                           log.info("{} exited due to idle timeout", this.getName());
                           break;
                       }
                   }

                   task.run();
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 以下是拒绝策略
     * 抛出异常的拒绝策略
     */
    public static class AbortRejectPolicy implements MyRejectedExecutionHandler {
        public AbortRejectPolicy() { }

        @Override
        public void rejectedExecution(Runnable task, MyThreadPoolExecutor executor) {
            throw new RuntimeException("Task " + task.toString() + " rejected");
        }
    }

    /**
     * 直接忽略的拒绝策略
     */
    public static class DiscardRejectPolicy implements MyRejectedExecutionHandler {
        public DiscardRejectPolicy() { }

        @Override
        public void rejectedExecution(Runnable task, MyThreadPoolExecutor executor) { }
    }

    /**
     * 记录日志的拒绝策略
     */
    public static class LogAndDropPolicy implements MyRejectedExecutionHandler {
        public LogAndDropPolicy() { }

        @Override
        public void rejectedExecution(Runnable task, MyThreadPoolExecutor executor) {
            log.info("Task {} rejected", task.toString());
        }
    }
}

@Slf4j
class TestMyThreadPool {
    public static void main(String[] args) {
        MyThreadPoolExecutor pool = new MyThreadPoolExecutor(
                2, // 核心线程数
                5, // 最大线程数
                5000L, // 非核心线程存活时间
                TimeUnit.MILLISECONDS, // keepAliveTime为5s
                3, // 队列容量
                new MyThreadPoolExecutor.LogAndDropPolicy()); // 使用日志记录的拒绝策略
        AtomicInteger taskId = new AtomicInteger();

        while (true) {
            int currentId = taskId.getAndIncrement();

            pool.execute(() -> {
                // 唯一的Runnable任务是打印一条通知
                log.info("{} executing task {}", Thread.currentThread().getName(), currentId);
                try {
                    Thread.sleep(1000); // 模拟任务耗时
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            });

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }
}
