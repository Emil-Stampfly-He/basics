package synchronizer.semaphore;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
public class MySemaphore {

    private int permits; // 许可数
    private final Deque<Thread> queue; // 队列

    public MySemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Permits cannot be negative.");
        }

        this.permits = permits;
        this.queue = new ArrayDeque<>();
    }

    public void acquire() {
        this.acquire(1);
    }

    /**
     * 一次性获取n个许可
     * @param n n个许可
     */
    public void acquire(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("N must be greater than zero.");
        }

        synchronized (this) {
            queue.addLast(Thread.currentThread()); // 进入队列末尾
            // 队头不是自己，说明还没轮到，必须等；或者许可数不足，也必须等
            try {
                while (queue.peekFirst() != Thread.currentThread() || permits < n) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                // 如果线程中途挂掉，必须将线程从队列中移出，否则后续线程永远无法成为队头
                queue.remove(Thread.currentThread());
                log.error(e.getMessage(), e);
            }

            permits -= n;
            queue.removeFirst();
        }
    }

    public void release() {
        this.release(1);
    }

    public void release(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("N must be greater than zero.");
        }

        synchronized (this) {
            permits += n;
            this.notifyAll();
        }
    }

    /**
     * 尝试立马获取锁
     * acquire是获取不到就阻塞，tryAcquire是获取不到就返回false
     * @return 立马获取失败则返回false，否则返回true
     */
    public boolean tryAcquire() {
        return this.tryAcquire(1);
    }

    public boolean tryAcquire(int n) {
        if (n <= 0) throw new IllegalArgumentException("N cannot be negative.");

        synchronized (this) {
            queue.addLast(Thread.currentThread());
            if (queue.peekFirst() == Thread.currentThread() && permits >= n) {
                permits -= n;
                return true;
            }

            return false;
        }
    }

}

@Slf4j
class TestMySemaphore {
    private static final int PERMITS = 100;

    public static void main(String[] args) {
        MySemaphore available = new MySemaphore(PERMITS);
        log.info("Main thread trying to acquire permit...");
        log.info("Result: {}", available.tryAcquire());

        for (int i = 0; i < PERMITS; i++) {
            new Thread(() -> {
                available.acquire();
                sleep(5000);
                available.release();
            }).start();
        }

        sleep(1000); // 这里让主线程睡眠一下，等待CPU调度子线程获取许可，否则CPU会来不及调度100个子线程
        log.info("Main thread trying to release permit again...");
        log.info("Result: {}", available.tryAcquire());

        sleep(10000);
        log.info("Main thread trying to release permit again...");
        log.info("Result: {}", available.tryAcquire());
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}

class TestMySemaphoreAlternate {
    public static void main(String[] args) throws InterruptedException {
        MySemaphore semA = new MySemaphore(3);
        semA.acquire(3); // permits = 0

        boolean secondA = semA.tryAcquire();
        Assertions.assertFalse(secondA);

        semA.release(2); // permits = 2

        boolean thirdA = semA.tryAcquire(); // permits = 1
        Assertions.assertTrue(thirdA);

        semA.acquire(2); // 1 < 2, 线程会被阻塞
    }
}

