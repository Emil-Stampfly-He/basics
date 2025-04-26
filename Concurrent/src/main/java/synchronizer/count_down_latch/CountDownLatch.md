# 从零开始的`CountDownLatch`

**Github: https://github.com/Emil-Stampfly-He/basics**

我们首先回顾一下`CountDownLatch`的作用：
1. 它维护了一个计数器，其初始值由构造器指定
2. 调用`await`的线程在计数器变成0之前会被阻塞
3. 每调用一次`countDown`，计数器就会减1
4. 计数器变成0时，所有阻塞的线程都会被唤醒

## 1. 基本构造
从上述的功能中可以看出，我们需要一个`count`字段当作计数器。这个计数器的值必须大于0才有意义：
```Java
public class MyCountDownLatch {
    private int count;

    public MyCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative.");
        }
        
        this.count = count;
    }
}
```

## 2. `await`
await方法的作用是：当计数器的值大于0时，线程应当一直处于阻塞（等待）状态：
```Java
public void await() throws InterruptedException {
    synchronized (this) {
        while (count > 0) {
            this.wait();
        }
    }
}
```
整体方法逻辑非常简单，但有一个非常容易错的地方：这里使用`while`循环来检查计数器的值而不是`if`。这是因为如果不使用`while`来判断，会造成线程**虚假唤醒**的现象。

什么是虚假唤醒？JVM规范允许线程在没有任何`notify`/`notifyAll`调用的情况下，从`wait`中无故返回一次，使得线程在不满足判断的条件下也被解除阻塞状态。因此，JVM规范强调，任何基于`wait`或`notify`的等待，都必须使用`while`循环做条件判断。

如果听上去比较难以理解，我们可以使用代码来对比`while`与`if`：

如果使用`if`：
```aiignore
public void someMethod(Object... args) throws InterruptedException {
    if (!condition) { 
        wait();-----
    }              |
}                  ∨
```
线程从`wait`中返回后就直接开始执行后面的逻辑。
而如果使用`while`，那么即使从`wait`中退出，也会重新循环检查。
```aiignore
public void someMethod(Object... args) throws InterruptedException {
    while (!condition) { <--|
        wait();             |
        ---------------------
    }
}
```

## 3. `countDown`
这个方法的意义是，每被调用一次，计数器就减1。如果计数器为0，则唤醒所有正在阻塞的线程。

逻辑同样非常简单：
```Java
public void countDown() {
    synchronized (this) {
        if (count > 0) {
            count--;
            
            if (countDown() == 0) {
                this.notifyAll();
            }
        }
    }
}
```

这样，我们就完成了`await`与`countDown`的相互配合。完整代码如下：
```Java
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
}
```

## 4. 测试
这里，我们使用Oracle官方的`CountDownLatch`示例来测试我们自己的`CountDownLatch`是否能够正常运行。

这个测试类希望实现的基本逻辑是：
1. 设置两个信号，`startSignal`给主线程进行count down，`endSignal`给子线程进行count down
2. 主线程在第一次做工作（调用`doSomeWork`方法）时，剩下的10个子线程都在阻塞中
3. 主线程将`startSignal`减到0后，其他线程开始工作，主线程也再次工作
4. 主线程工作完后被阻塞，直到子线程将`endSignal`减到0为止
```Java
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
```
运行结果如下：
```aiignore
09:48:35.589 [main] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- main is doing some work
09:48:40.602 [Thread-9] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-9 is doing some work
09:48:40.602 [Thread-0] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-0 is doing some work
09:48:40.602 [Thread-4] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-4 is doing some work
09:48:40.602 [Thread-8] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-8 is doing some work
09:48:40.602 [Thread-3] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-3 is doing some work
09:48:40.602 [Thread-6] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-6 is doing some work
09:48:40.602 [Thread-5] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-5 is doing some work
09:48:40.603 [Thread-1] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-1 is doing some work
09:48:40.602 [main] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- main is doing some work
09:48:40.602 [Thread-7] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-7 is doing some work
09:48:40.603 [Thread-2] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- Thread-2 is doing some work
09:48:45.618 [main] INFO synchronizer.count_down_latch.TestMyCountDownLatchAlternate -- All done.
```

运行结果与我们所预期的行为完全一致。
