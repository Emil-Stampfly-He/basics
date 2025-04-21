package blocking_queue;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyArrayBlockingQueue<T> {
    private final T[] items;
    private int head = 0;
    private int tail = 0;
    private int size = 0;
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

@Slf4j
class TestMyArrayBlockingQueue {
    public static void main(String[] args) {
        MyArrayBlockingQueue<Integer> queue = new MyArrayBlockingQueue<>(5);

        Thread producer = new  Thread(() -> {
            int i = 1;
            try {
                while (true) {
                    log.info("Producing: {}", i);
                    queue.put(i++);
                    // 生产速度是消费速度的2倍
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    Integer item = queue.take();
                    log.info("Consuming: {}", item);
                    // 由于消费速度小于生产速度，数组写满后会变成生产与消费交替进行
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        producer.start();
        consumer.start();
    }
}
