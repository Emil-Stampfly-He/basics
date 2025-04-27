package synchronizer.semaphore;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
public class MySemaphore {

    private final Sync sync;

    /**
     * 委托类：Sync
     * 默认实现：不公平信号量
     */
    private static class Sync {

        // 将许可数委托给Sync
        // 设置为protected是为了让子类拿到父类的permits
        protected int permits;

        Sync(int permits) {
           this.permits = permits;
        }

        public void acquire() throws InterruptedException {
            this.acquire(1);
        }

        public void acquire(int n) throws InterruptedException {
            if (n <= 0) {
                throw new IllegalArgumentException("N must be greater than zero.");
            }

            synchronized (this) {
                while (permits < n) {
                    this.wait();
                }

                permits -= n;
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

        public boolean tryAcquire() {
            return this.tryAcquire(1);
        }

        public boolean tryAcquire(int n) {
            if (n <= 0) throw new IllegalArgumentException("N cannot be negative.");

            synchronized (this) {
                if (permits >= n) {
                    permits -= n;
                    return true;
                }

                return false;
            }
        }
    }

    /**
     * 非公平信号量实现
     * 由于父类Sync的默认实现就是非公平的，NonfairSync只要继承即可
     */
    private static final class NonfairSync extends Sync {
        NonfairSync(int permits) {
            super(permits);
        }
    }

    /**
     * 公平信号量实现
     * 需要重载acquire(int n)和tryAcquire(int n)方法
     */
    private static final class FairSync extends Sync {
        private final Deque<Thread> queue = new ArrayDeque<>();

        FairSync(int permits) {
            super(permits);
        }

        @Override
        public void acquire(int n) throws InterruptedException {
            if (n <= 0) {
                throw new IllegalArgumentException("N must be greater than zero.");
            }

            Thread cur = Thread.currentThread();
            synchronized (this) {
                queue.addLast(cur);
                try { // 队头不是自己，说明还没轮到，必须等；或者许可数不足，也必须等
                    while (queue.peekFirst() != cur || permits < n) {
                        this.wait();
                    }

                    permits -= n;
                } finally {
                    // 不管正常返回还是异常，都必须将自己移出队列
                    queue.remove(cur);
                }
            }
        }

        @Override
        public boolean tryAcquire(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("N cannot be negative.");
            }

            Thread cur = Thread.currentThread();
            synchronized (this) {
                queue.addLast(cur);

                if (queue.peekFirst() != cur || permits < n) {
                    queue.remove(cur);
                    return false;
                }

                permits -= n;
                queue.remove(cur);
                return true;
            }
        }
    }




    /**
     * MySemaphore的构造方法
     * @param permits 许可数
     */
    public MySemaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    public MySemaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }




    /* 以下均暴露给调用者 */
    public void acquire() throws InterruptedException {
        sync.acquire();
    }

    public void acquire(int n) throws InterruptedException {
        sync.acquire(n);
    }

    public void release() {
        sync.release();
    }

    public void release(int n) {
        sync.release(n);
    }

    public boolean tryAcquire() {
        return sync.tryAcquire(1);
    }

    public boolean tryAcquire(int n) {
        return sync.tryAcquire(n);
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
                try {
                    available.acquire();
                    sleep(5000);
                    available.release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        sleep(3000); // 这里让主线程睡眠一下，等待CPU调度子线程获取许可，否则CPU会来不及调度100个子线程
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

