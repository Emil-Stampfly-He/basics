package synchronizer.count_down_latch;

import lombok.extern.slf4j.Slf4j;

public class MyCountDownLatch {

    private int count;

    public MyCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative.");
        }

        this.count = count;
    }

    public void await() throws InterruptedException {
        synchronized (this) {
            while (count > 0) {
                this.wait();
            }
        }
    }

    public void countDown() {
        synchronized (this) {
            if (count > 0) {
                count--;
                if (count == 0) {
                    this.notifyAll();
                }
            }
        }
    }

    public int getCount() {
        synchronized (this) {
            return count;
        }
    }
}

@Slf4j
class TestMyCountDownLatchAlternate {
    public static void main(String[] args) throws InterruptedException {
        int nThread = 10;
        MyCountDownLatch startSignal = new MyCountDownLatch(1);
        MyCountDownLatch endSignal = new MyCountDownLatch(nThread);

        for (int i = 0; i < nThread; i++) {
            new Thread(() -> {
                try {
                    startSignal.await();
                    doSomeWork(10000);
                    endSignal.countDown();
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }).start();
        }

        doSomeWork(5000); // 剩下的10个新线程都在阻塞中
        startSignal.countDown(); // 主线程将计数器减到0，其他线程开始工作
        doSomeWork(5000);
        endSignal.await(); // 主线程阻塞，直到10个新线程完成工作后调用计数器，将计数器减到0

        log.info("All done.");
    }

    public static void doSomeWork(long consumingTime) {
        try {
            log.info("{} is doing some work", Thread.currentThread().getName());
            Thread.sleep(consumingTime);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
