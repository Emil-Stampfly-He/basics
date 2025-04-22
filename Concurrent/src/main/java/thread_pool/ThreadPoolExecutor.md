# 从零开始的`ThreadPoolExecutor`

**Github: https://github.com/Emil-Stampfly-He/basics**

我们会分成5步搭建一个功能尽可能完善的线程池：
1. 含有固定数量工作的线程 + 任务队列
2. 支持任务拒绝策略
3. 支持动态线程增长 & 回收
4. 支持`shutdown` & `awaitTermination`，即生命周期控制
5. 提供`submit`方法返回`Future`，即`Callable` + `Future`

## 1. 拥有固定数量工作的线程 + 任务队列
一个最小化的线程池必须包含三个功能：接受任务、缓存队列，以及派发给工作线程。

我们希望线程池中的线程执行逻辑是固定的、可管理的，因此，我们创建的线程需要拥有相同的执行逻辑.
最好的做法是将这些线程存储在一个集合或数组中，然后遍历指定行为。

先来看看基本架构：既然要创建一个最简单的线程池，那么需要一个管理任务的容器：任务队列，以及一个管理线程的容器：

```Java
public class MyThreadPoolExecutor {
    
    private final BlockingQueue<Runnable> taskQueue;
    private Thread[] threads;
}
```
光有线程还不行，我们必须给线程统一指定执行动作，线程才能知道要如何工作。
一个`Runnable`对象可以看成是一个线程所要执行的任务。`new Thread(Runnable command)`其实就是给线程指定动作。

```Java
int poolSize; // 根据用户传过来的poolSize指定大小
int nThreadNum; // 根据用户传过来的nThreadNum指定线程池中线程的数量
taskQueue = new LinkedBlockingQueue<>(poolSize); // 任务队列
threads = new Thread[nThreadNum]; // 线程池中的线程

Runnable command = () -> {
    try {
        while (true) {
            Runnable task = taskQueue.take(); // 任务队列中的任务，阻塞地获取任务
            task.run();
        }
    } catch (Exception e) {
        log.err(e.getMessage());
    }
}; // 线程池中线程要执行的动作指令
        
for (int i = 0; i < nThreadNum; i++) {
    threads[i] = new Thread(command); // 将带有指定动作的线程加入线程池（数组）中
    thread[i].start(); // 启动线程
}
```

上面的这些看上去比较繁琐，我们可以为线程池中的那些线程封装成`Worker`类。加一个指定线程名字的构造方法有利于测试的进行。
注意，一个`Worker`实际上还是一个线程，只不过名字从`Thread`换成了`Worker`。
```Java
private class Worker extends Thread {
    public Worker(String name) {
        super(name);
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                Runnable task = taskQueue.take(); // 阻塞地获取任务
                task.run(); //执行任务
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
```
所以我们的线程池的基本结构可以写成：

```Java
public class MyThreadPoolExecutor {

    private final BlockingQueue<Runnable> taskQueue;
    private final Worker[] workers;

    public MyThreadPoolExecutor(int poolSize, int nThreadNum) {
        taskQueue = new LinkedBlockingQueue<>(poolSize); // 也可以是ArrayBlockingQueue
        workers = new Worker[nThreadNum];
        
        for (int i = 0; i < nThreadNum; i++) {
            workers[i] = new Worker("worker-" + i);
            workers[i].start();
        }
    }

//    private class Worker extends Thread {
//        public Worker(String name) {
//            super(name);
//        }
//
//        @Override
//        public void run() {
//            try {
//                while (true) {
//                    Runnable task = taskQueue.take(); // 阻塞地获取任务
//                    task.run(); //执行任务
//                }
//            } catch (InterruptedException e) {
//                log.error(e.getMessage());
//            }
//        }
//    }
}
```
这里的`LinkedBlockingQueue`也可以换成`ArrayBlockingQueue`。两者的区别会在最后讨论。

指定完线程池的基本结构后，我们需要为线程池创建调度任务的方法：`execute`。 由于目前我们想创建一个极简版本的线程池，
我们只将`execute`方法定义为一个单纯向任务队列中添加任务的方法。这个方法可以是`offer`或`put`。我们稍后在测试线程池行为时会对比这两种方法。
```Java
public void execute(Runnable task) {
    // 使用offer，任务队列满不会阻塞
    // put则会在队列满后阻塞，直到队列非满才会向队列中再次添加任务
    taskQueue.execute(task);
}
```
至此，一个完成的最小线程池模型就创建好了。它包括了**接收任务 → 缓存队列 → 派发给工作线程**三个核心功能：
```Java
@Slf4j
public class MyThreadPoolExecutor {
    private final BlockingQueue<Runnable> taskQueue;

    public MyThreadPoolExecutor(int poolSize, int nThreadNum) {
        taskQueue = new LinkedBlockingQueue<>(5);
        Worker[] workers = new Worker[nThreadNum];

        for (int i = 0; i < nThreadNum; i++) {
            workers[i] = new Worker("worker-" + i);
            workers[i].start();
        }
    }

    public void execute(Runnable task) {
        taskQueue.offer(task); // 不考虑拒绝策略
        // 使用offer，任务队列满不会阻塞；put则会阻塞
        // 会导致测试类行为差异非常大
    }


    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable task = taskQueue.take(); // 阻塞地获取任务
                    task.run(); //执行任务
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
    }
}
```
我们可以写一个测试类来看看这个自定义最简化的线程池的行为：
```Java
@Slf4j
class TestMyThreadPool {
    public static void main(String[] args) {
        MyThreadPoolExecutor pool = new MyThreadPoolExecutor(3, 3);
        AtomicInteger taskId = new AtomicInteger();

        while (true) {
            int currentId = taskId.getAndIncrement();

            pool.execute(() -> {
                // 唯一的任务是打印一条通知并睡眠1s
                log.info("{} executing task {}", Thread.currentThread().getName(), currentId);
                try {
                    Thread.sleep(1000); // 模拟任务耗时
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            });
        }
    }
}
```
运行结果如下：
```aiignore
14:48:40.837 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 2
14:48:40.837 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 0
14:48:40.837 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 1
14:48:41.845 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 3
14:48:41.845 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 23
14:48:41.845 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 4
14:48:42.855 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 24
14:48:42.855 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 169584178
14:48:42.855 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 44
14:48:43.863 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 276103748
14:48:43.863 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 169584180
14:48:43.863 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 169584179
14:48:44.865 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 276106658
14:48:44.865 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 276111114
14:48:44.866 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 385859508
14:48:45.865 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 385862734
```
可以看到，`currentId`增长得非常快，如果再运行久一点甚至会发生`Integer`超界现象。这是因为我们的`execute`方法使用的是非阻塞式的`offer`。
线程池在添给任务队列添加任务的速度是非常快的，这就会导致工作线程在睡眠期间，`currentId`已经被增加了上百万甚至亿次，也就是往任务队列中写了这么多次任务。
但`offer`的特点是：如果`BlockingQueue`已满，元素就会被放弃添加，并返回一个`false`。这就意味着中间很多任务都被放弃写入，且我们没有被通知。

我们可以尝试将`offer`更改成`put`来看看会发生什么。由于`put`的特点是：当`BlockingQueue`满时，阻塞式等待任务写入，所以我们可以预计，`currentId`会按顺序增长。

运行结果如下：
```aiignore
14:59:18.250 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 0
14:59:18.250 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 1
14:59:18.250 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 2
14:59:19.261 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 3
14:59:19.262 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 5
14:59:19.261 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 4
14:59:20.266 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 6
14:59:20.266 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 8
14:59:20.266 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 7
14:59:21.267 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 9
14:59:21.268 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 10
14:59:21.268 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 11
14:59:22.267 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 12
14:59:22.275 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 13
14:59:22.275 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 14
14:59:23.275 [worker-0] INFO thread_pool.TestMyThreadPool -- worker-0 executing task 15
14:59:23.289 [worker-1] INFO thread_pool.TestMyThreadPool -- worker-1 executing task 16
14:59:23.289 [worker-2] INFO thread_pool.TestMyThreadPool -- worker-2 executing task 17
```
与预期一致。

既然`put`方法能够“不跳号”执行，为什么我们要使用`offer`，连官方版本也是使用`offer`而不使用`put`呢？
```Java
// java.util.concurrent
public void execute(Runnable command) {
    // ...
    
    // 使用了offer
    if (isRunning(c) && workQueue.offer(command)) {
        // ...
    }
    else if (!addWorker(command, false))
        reject(command); // 拒绝策略
}
```
这是为了避免阻塞调用线程。当队列已满时，提交任务的线程会被阻塞，可能导致资源被耗尽，进而引发系统级故障。
而`offer`的立即返回特性允许线程池在队列满时快速判断是否需要创建新线程（官方版本的实现），且可以触发拒绝策略，实现降级逻辑（fallback），
使得开发者对队列满时的处理更加灵活。

注意到，官方版本的最后有一个`reject`方法。接下来，我们给自己实现的最小化线程池加上拒绝策略，让我们的线程池能够灵活处理队列满时的情况。

## 2. 支持任务拒绝策略
前面提到，如果使用`offer`方法，那么很多任务就会被隐形地丢弃，而且不会抛出任何异常。这对于生产环境是非常不利的，因为使用者无法知道初始任务队列容量是否充足。
所以我们需要定义任务拒绝策略，让线程池能够及时提醒我们容量的不足。

我们可以先定义一个拒绝策略接口，之后的拒绝策略具体实现类都可以继承这个接口：
```Java
public interface MyRejectedExecutionHandler {
    void rejectedExecution(Runnable task, MyThreadPoolExecutor executor);
}
```

拒绝策略有很多种，我们这里挑选几种简单的进行实验：抛出异常、直接忽略，以及日志记录的拒绝策略。
我们可以为这三种拒绝策略分别实现三个类，这三个类均继承`MyRejectedExecutionHandler`：
```Java
@Slf4j
public class MyThreadPoolExecutor {
    
    // ...
    
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
     * 直接忽略的拒绝策略，也就是什么都不做
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
```
由于在初始化线程池时，拒绝策略只需要指定一次，所以可以将拒绝策略的类设置成为公共静态内部类。

我们可以再次运行我们的测试方法，只需要在最开始创建线程池时传入`new MyThreadPoolExecutor.LogAndDropPolicy()`参数即可。最终的输出结果是：
```aiignore
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@5a265643 rejected
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@40917e6a rejected
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@78132292 rejected
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@62ab9215 rejected
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@a57444d rejected
18:17:20.738 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$19/0x00000216c50188a8@417e0943 rejected
```
可以看到大量的任务被拒绝，然后以日志的形式在控制台输出。

官方还有一个实用的拒绝策略`DiscardOldestPolicy`。字面意思，丢弃最早的任务：
```Java
public static class DiscardOldestPolicy implements RejectedExecutionHandler {
    public DiscardOldestPolicy() { }
    
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        if (!e.isShutdown()) {
            e.getQueue().poll();
            e.execute(r);
        }
    }
}
```
至此，我们成功地给线程池加上了拒绝策略，实现了降级逻辑。目前为止的完整代码：

```Java
@Slf4j
public class MyThreadPoolExecutor {
    private final BlockingQueue<Runnable> taskQueue;
    private final MyRejectedExecutionHandler rejectedExecutionHandler;

    public MyThreadPoolExecutor(int poolSize, int nThreadNum, MyRejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        taskQueue = new LinkedBlockingQueue<>(poolSize);
        Worker[] workers = new Worker[nThreadNum];

        for (int i = 0; i < nThreadNum; i++) {
            workers[i] = new Worker("worker-" + i);
            workers[i].start();
        }
    }

    public void execute(Runnable task) {
        boolean success = taskQueue.offer(task);
        if (!success) {
            // 执行拒绝策略
            rejectedExecutionHandler.rejectedExecution(task, this);
        }
    }


    private class Worker extends Thread {
        public Worker(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable task = taskQueue.take(); // 阻塞地获取任务
                    task.run(); //执行任务
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
```

## 3. 支持动态线程增长 & 回收
工作负载不是平均的，线程池如果不能根据负载变化动态增减线程，就会要么浪费资源，要么处理不过来任务。
因此，我们希望线程池是弹性的：
* 如果任务队列满了，且当前线程数 < 最大线程数，则新增线程处理任务
* 如果某个线程在`keepAliveTime`没有接到任务，它就会自动退出（被销毁）
* 保留核心线程`corePoolSize`个，其他的均可回收

我们可以在已有的线程池的基础上添加以下字段：
```Java
private final int coreSize; // 核心线程数
private final int maxSize;  // 最大线程数
private final long keepAliveTime; // 非核心线程空闲时的最大存活时间（ms）
private final BlockingQueue<Runnable> taskQueue; // 任务队列
private final MyRejectedExecutionHandler rejectedExecutionHandler; // 拒绝策略

private final HashSet<Worker> workers = new HashSet<>(); // 现在使用HashSet来装工作线程
private final Object lock = new Object(); // 稍后解释这个lock

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
```
为了区分核心线程与非核心线程，我们可以为`Worker`指定一个`isCore`参数来加以区分。
因此对于`addWorker`方法，可以也用`isCore`添加核心线程或非核心线程。而
在线程池初始化时，只创建核心线程。只有满足一定条件（前面有提到）的时候才会创建非核心线程：

```Java
private void addWorker(boolean isCore) {
    // isCore可以用于指定创建的是核心线程还是非核心线程
    Worker worker = new Worker("worker-" + workers.size(), isCore);
    synchronized (lock) { // 锁
        workers.add(worker);
    }
    
    worker.start();
}

private class Worker extends Thread {
    
    private final boolean core;
    
    public Worker(String name, boolean core) {
        super(name);
        this.core = core; // 可用于区分核心线程与非核心线程
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
```
这里有两个需要解释的点：

1. `poll(keepAliveTime, unit)`

这是`BlockingQueue`提供的带超时阻塞的获取任务的方法。如果队列中已经有元素，就返回这个元素，不等待。
如果队列是空的，则最多等待`keepAliveTime`这么久（`unit`是时间单位）。如果等了这么久队列中还是没有任务，就直接返回`null`。
接着就可以使用是否返回`null`来判断是否需要删除这个线程。这也是为什么不使用`remove`或`take`方法的原因。
如果使用`take`则非核心线程永远不会退出，而使用`remove`则会导致直接抛出异常。两种情况都不是我们希望发生的。

2. `synchronized`互斥锁

这个锁的意义是保证`workers`集合的线程安全，避免多个线程同时修改它造成数据不一致或崩溃的情况。在这一段代码中：
```Java
if (task == null) {
    synchronized (lock) {
        workers.remove(this);
    }

    log.info("{} exited due to idle timeout", this.getName());
    break;
}
```
多个线程可能会同时尝试退出，调用`workers.remove(this)`。由于`HashSet`不是线程安全集合，不加锁可能会出现：

* `ConcurrentModificationException`
* `remove`操作失败
* `workers`状态不一致

的情况。当然，我们可以使用线程安全的集合进行改写：

* `ConcurrentHashMap<Worker, Boolean>`，`Boolean`用于记录工作线程是核心的还是非核心的
* `CopyOnWriteArraySet<Worker>`

在写完线程池中线程增加与销毁逻辑后，最后一个要更改的是`execute`方法。先来回忆一下原先的`execute`方法：
```Java
public void execute(Runnable task) {
    boolean success = taskQueue.offer(task);
    if (!success) { // 任务队列满
        // 走拒绝策略
        rejectedExecutionHandler.rejectedExecution(task, this);
    }
}
```
现在，因为线程池的线程数量是弹性的，所以在滑退到拒绝策略之前，还需要判断一下，能否增加一些线程来解决任务队列满的情况。
具体来说，如果任务队列满，而且线程池中的工作线程总数没有达到`maxSize`，则创建非核心线程：
```Java
public void execute(Runnable task) {
    boolean success = taskQueue.offer(task);
    if (!success) { // 任务队列满
        synchronized (lock) {
            if (workers.size() < maxSize) {
                addWorker(false); // 创建一个非核心工作线程
                taskQueue.offer(task); // 重新将任务写入队列
                return;
            }
        }
        
        // 线程池已达最大负荷，走拒绝策略
        rejectedExecutionHandler.rejectedExecution(task, this);
    }
}
```
这里的`synchronized`关键字同`addWorker`方法中的作用一样，用来保证`workers`集合的一致性。

到此，线程池的伸缩性就写完了。我们来看看到目前为止我们的线程池的代码：实现了基本功能、拒绝逻辑，以及伸缩性。
```Java
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
        Worker worker = new Worker("worker-" + workers.size(), isCore);
        synchronized (lock) {
            workers.add(worker);
        }

        worker.start();
    }
    
// 如果想在测试类中看得更清晰，可以这样创建线程。本质上跟上面的addWorker方法没有任何区别
//    private void addWorker(boolean isCore) {
//        Worker worker;
//        if (isCore) {
//            worker = new Worker("core-worker-" + workers.size(), true);
//        } else {
//            worker = new Worker("non-core-worker-" + workers.size(), false);
//        }
//        synchronized (lock) {
//            workers.add(worker);
//        }
//
//        worker.start();
//    }


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
```
修改一下我们之前的测试类。运行后可以看到，由于主线程向`taskQueue`中添加任务快于工作线程的处理速度，所以有3个非核心工作线程被创建：
```Java
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
```
运行，输出结果是：
```aiignore
23:40:20.147 [core-worker-0] INFO thread_pool.TestMyThreadPool -- core-worker-0 executing task 0
23:40:20.252 [core-worker-1] INFO thread_pool.TestMyThreadPool -- core-worker-1 executing task 1
23:40:20.676 [non-core-worker-2] INFO thread_pool.TestMyThreadPool -- non-core-worker-2 executing task 2
23:40:20.878 [non-core-worker-3] INFO thread_pool.TestMyThreadPool -- non-core-worker-3 executing task 3
23:40:21.080 [non-core-worker-4] INFO thread_pool.TestMyThreadPool -- non-core-worker-4 executing task 4
23:40:21.156 [core-worker-0] INFO thread_pool.TestMyThreadPool -- core-worker-0 executing task 6
23:40:21.262 [core-worker-1] INFO thread_pool.TestMyThreadPool -- core-worker-1 executing task 8
23:40:21.513 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$26/0x000001feb901c6b8@47089e5f rejected
23:40:21.621 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$26/0x000001feb901c6b8@4141d797 rejected
23:40:21.678 [non-core-worker-2] INFO thread_pool.TestMyThreadPool -- non-core-worker-2 executing task 10
23:40:21.841 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$26/0x000001feb901c6b8@68f7aae2 rejected
23:40:21.878 [non-core-worker-3] INFO thread_pool.TestMyThreadPool -- non-core-worker-3 executing task 11
23:40:22.061 [main] INFO thread_pool.MyThreadPoolExecutor -- Task thread_pool.TestMyThreadPool$$Lambda$26/0x000001feb901c6b8@4f47d241 rejected
23:40:22.083 [non-core-worker-4] INFO thread_pool.TestMyThreadPool -- non-core-worker-4 executing task 12
23:40:22.156 [core-worker-0] INFO thread_pool.TestMyThreadPool -- core-worker-0 executing task 15
23:40:22.266 [core-worker-1] INFO thread_pool.TestMyThreadPool -- core-worker-1 executing task 17
```
可以看到3个非核心线程因为任务队列满而被创建，同时因为任务写入太快且线程池达到最大负荷，一部分任务直接走了拒绝策略。