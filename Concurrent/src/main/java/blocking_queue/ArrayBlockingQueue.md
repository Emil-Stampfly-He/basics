# 从零开始的`ArrayBlockingQueue`

## 入队与出队的三组方法
`BlockingQueue`中有三组加入与删除的方法：
* `put` & `take`

队列满或空会阻塞线程，直到能够再次写入或移出。
* `add` & `remove`

队列满或队列空时尝试写入或移出元素，会抛出异常。
* `offer` & `poll`

队列满或队列空时尝试写入或移出元素，会失败。`offer`返回`false`，`poll`返回`null`。

除了`put`与`take`方法，其他两组实现起来都非常简单，只需要做出边界条件判断即可：
```Java
if (size == capacity) {
    throw new IllegalStateException("Queue full."); // add
    // 或者
    return false; // offer 
}
```
```Java
if (size == 0) {
    throw new IllegalStateException("Queue empty."); // remove
    // 或者
    return true; // poll
}
```

## 为什么要使用循环数组？
因为我们想构建一个固定容量的队列，且它能够同时保持：
* 内存占用恒定（避免频繁扩容或拷贝数组移动元素）
* 高效的入队与出队操作（$O(1)时间复杂度$）

我们可以维护两个指针：`head`和`tail`，同时维护`size`变量， 做到正确判断阻塞与释放条件。
为了让一个数组成为循环数组，我们需要在指针达到末尾时，下一步移动到数组开头：
```aiignore
head = (head + 1) % capacity
tail = (tail + 1) % capacity
```

## 使用`synchronized`关键字上锁
使用`synchronized`关键字上锁是最直接的方式。
```Java
public class MyArrayBlockingQueue<T> {
    private final T[] items;
    public int head = 0;
    public int tail = 0;
    public int size = 0;
    private final int capacity;

    @SuppressWarnings("unchecked")
    public MyArrayBlockingQueue(int capacity) {
        this.capacity = capacity;
        items = (T[]) new Object[capacity]; // 循环数组
    }

    /**
     * 第一组：put & take
     * 阻塞
     * @param t 入队/出队的元素
     * @throws InterruptedException 中断异常
     */
    public synchronized void put(T t) throws InterruptedException {
        while (size == capacity) {
            // 队列满，等待写入
            wait();
        }

        items[tail] = t;
        if (++tail == capacity) {
            tail = 0;
        } // 等价于 tail = (tail + 1) % capacity
        size++;

        // 唤醒在take()中等待的线程
        notifyAll();
    }

    public synchronized T take() throws InterruptedException {
        while (size == 0) {
            // 队列空，等待读取
            wait();
        }

        T t = items[head];
        if (++head == capacity) {
            head = 0;
        } // 等价于 head = (head + 1) % capacity
        size--;

        // 唤醒在put()中等待的线程
        notifyAll();
        return t;
    }

    /**
     * 第二组：add & remove
     * 抛出异常
     * @param t 入队/出队的元素
     * @throws InterruptedException 中断异常
     */
    public synchronized void add(T t) throws InterruptedException {
        if (size == capacity) {
            throw new IllegalStateException("Queue full");
        }

        items[tail] = t;
        tail = (tail + 1) % capacity;
        size++;
    }

    public synchronized T remove() throws InterruptedException {
        if (size == 0) {
            throw new IllegalStateException("Queue empty");
        }

        T t = items[head];
        items[head] = null;
        head = (head + 1) % capacity;
        size--;
        return t;
    }

    /**
     * 第三组：offer & poll
     * 失败，返回false或空
     * @param t 入队/出队的元素
     * @return false或空
     * @throws InterruptedException 中断异常
     */
    public synchronized boolean offer(T t) throws InterruptedException {
        if (size == capacity) {
            return false;
        }

        items[tail] = t;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }

    public synchronized T poll() throws InterruptedException {
        if (size == 0) {
            return null;
        }

        T t = items[head];
        items[head] = null;
        head = (head + 1) % capacity;
        size--;
        return t;
    }
}
```
## `ReentrantLock` + `Condition`
这也是官方实现ArrayBlockingQueue的方法。相比于`synchronized` + `wait`/`notifyAll`，
使用`ReentrantLock` + `Condition`有以下几个优势：
1. 可以定义多个Condition，控制更加精准
2. `lock.lockInterruptibly()`方法非常灵活，允许在等待获取锁的过程中被中断，任务可取消。
也就是说，对于`lock.lock()`方法，即使调用了`thread.interrupt()`方法，线程也不会放弃锁。这样容易出现死锁。
3. `ReentrentLock`可重入、可中断（第2点）、且具有公平性
   1. 可重入
    ```Java
    lock.lock();  // 第一次获取
    lock.lock();  // 第二次获取，同一个线程，不会死锁
    ...
    lock.unlock(); // 第一次释放
    lock.unlock(); // 第二次释放，锁才真正释放
    ```
   当然，`synchronized`也是可重入的，所以这一点两者相同。
   2. 可中断
   这一点在第2条已经讲过，不再赘述
   3. 公平性
   `synchronized`是非公平锁，这意味着JVM不能保证先到先得。虽然是性能高，但是可能导致线程饥饿。
   而`ReentrantLock`允许设置公平模式：
   ```Java
    ReentrantLock lock = new ReentrantLock(true);
   ```
   公平锁的好处是先到先得，但是性能会稍微低一点点，因为会维护一个等待队列。

以下是使用`ReentrantLock` + `Condition`实现的`ArrayBlockingQueue`：
```Java
public class MyAnotherArrayBlockingQueue<T> {

    private final T[] items;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition(); // 等待非满
    private final Condition notEmpty = lock.newCondition(); // 等待非空

    @SuppressWarnings("unchecked")
    public MyAnotherArrayBlockingQueue(int capacity) {
        this.capacity = capacity;
        items = (T[]) new Object[capacity];
    }

    public void put(T t) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size == capacity) {
                // 队列满，等待notFull信号
                notFull.await();
            }

            items[tail] = t;
            tail = (tail + 1) % capacity;
            size++;

            // 通知在take等待的线程
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (size == 0) {
                // 队列空，等待notEmpty信号
                notEmpty.await();
            }

            T t = items[head];
            head = (head + 1) % capacity;
            size--;

            // 通知在put等待的线程
            notFull.signal();
            return t;
        }  finally {
            lock.unlock();
        }
    }
}
```
可以写一个测试类来测试其行为：
```Java
@Slf4j
class TestMyAnotherArrayBlockingQueue {
    public static void main(String[] args) {
        MyAnotherArrayBlockingQueue<Integer> queue = new MyAnotherArrayBlockingQueue<>(3);

        // Producer
        new Thread(() -> {
            int i = 1;
            try {
                while (true) {
                    queue.put(i++);
                    log.info("Produced: {}", i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }).start();

        // Consumer
        new Thread(() -> {
            try {
                while (true) {
                    int item = queue.take();
                    log.info("Consumed: {}", item);
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }).start();
    }
}
```