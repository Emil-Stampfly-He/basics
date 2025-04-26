package sets.thread_pool;

public interface MyRejectedExecutionHandler {
    void rejectedExecution(Runnable task, MyThreadPoolExecutor executor);
}
