package synchronizer.semaphore;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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
        // 设置为volatile是为了保证在CAS操作中多线程之间的可见性
        protected volatile int permits;

        // 可以使用Unsafe，但Unsafe不属于公开API
        // 转为使用VarHandle，同样支持使用底层指令来进行原子操作
        private static final VarHandle V;

        static {
            try {
                // MethodHandles.lookup().findVarHandle()来创建一个VarHandle对象
                // Sync.class表明要操作的变量再Sync类中
                // “permits”是变量名
                // int.class是permits的类型
                V = MethodHandles.lookup().findVarHandle(Sync.class, "permits", int.class);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }


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

//            synchronized (this) {
//                while (permits < n) {
//                    this.wait();
//                }
//
//                permits -= n;
//            }

            while (true) {
                int available = this.permits;
                int remaining = available - n;
                if (remaining < 0) {
                    // 许可暂时不够，自旋等待
                    // 注意不得使用this.wait()，因为this不再代表当前线程
                    Thread.onSpinWait();
                    continue;
                }

                if (V.compareAndSet(this, available, remaining)) {
                    break;
                }
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

        @SuppressWarnings("unused")
        public boolean tryAcquire() {
            return this.tryAcquire(1);
        }

        public boolean tryAcquire(int n) {
            if (n <= 0) throw new IllegalArgumentException("N cannot be negative.");

//            synchronized (this) {
//                if (permits >= n) {
//                    permits -= n;
//                    return true;
//                }
//
//                return false;
//            }

            while (true) {
                int available = this.permits;
                int remaining = available - n;
                if (remaining < 0) {
                    return false;
                }

                if (V.compareAndSet(this, available, remaining)) {
                    return true;
                }
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
     * TODO 公平信号量需要改为CAS操作
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
class TestMySemaphoreFirst {
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

@Slf4j
class TestMySemaphoreSecond {
    public static void main(String[] args) throws InterruptedException {
        MySemaphore semA = new MySemaphore(3);
        semA.acquire(3); // permits = 0
        log.info("1 passed");

        boolean secondA = semA.tryAcquire();
        assertFalse(secondA);
        log.info("2 passed");

        semA.release(2); // permits = 2

        boolean thirdA = semA.tryAcquire(); // permits = 1
        assertTrue(thirdA);
        log.info("3 passed");

        semA.acquire(2); // 1 < 2, 线程会被阻塞
    }
}

class TestMySemaphoreThird {

    @Test
    public void testSingleThreadAcquireRelease() throws InterruptedException {
        MySemaphore sem = new MySemaphore(2);
        // 两次 acquire 都能拿到
        sem.acquire();
        sem.acquire();
        // 此时所有许可证用完，tryAcquire 应返回 false
        assertFalse(sem.tryAcquire());
        // 归还一个
        sem.release();
        // 现在又能拿到一个
        assertTrue(sem.tryAcquire());
    }

    @Test
    void testTryAcquireMultiple() {
        MySemaphore sem = new MySemaphore(3);
        // 一次性尝试拿 2 个
        assertTrue(sem.tryAcquire(2));
        // 剩余 1，试拿 2 个失败
        assertFalse(sem.tryAcquire(2));
        // 剩余仍是 1
        assertTrue(sem.tryAcquire(1));
    }

    @Test
    void testNonfairConcurrency() throws InterruptedException {
        final int THREADS = 10;
        MySemaphore sem = new MySemaphore(3, false);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);

        // 记录实际并发运行的最大线程数
        AtomicInteger maxConcurrent = new AtomicInteger();
        AtomicInteger current = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    sem.acquire();
                    int running = current.incrementAndGet();
                    maxConcurrent.getAndUpdate(prev -> Math.max(prev, running));
                    // 模拟工作
                    Thread.sleep(100);
                    current.decrementAndGet();
                    sem.release();
                } catch (Exception e) {
                    fail(e);
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        done.await();
        // 最多三个线程应该同时持有许可
        assertTrue(maxConcurrent.get() <= 3);
    }
}

