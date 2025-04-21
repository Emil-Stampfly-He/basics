package blocking_queue;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

