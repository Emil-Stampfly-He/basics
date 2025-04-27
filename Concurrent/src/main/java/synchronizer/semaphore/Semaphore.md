# 从零开始的`Semaphore`

更美观清晰的版本在：[GitHub](https://github.com/Emil-Stampfly-He/basics)

`Semaphore`（信号量）是用来控制同时访问某组共享资源的线程数量，它与`CountDownLatch`类似：
1. 构造时指定一个初始许可数`permits`，代表同时能有多少线程并发通过
2. 每个线程要进入前，必须先从信号量拿到一个许可。如果当前没有剩余许可，该线程就会阻塞等待
3. 用完之后，线程再把许可归还给信号量，唤醒等待中的线程

信号量可以很好地限制并发访问量。例如：
* 数据库连接池中有限的连接
* 网路接口限流

接下来我们从零开始，按照下面的步骤来实现一个功能尽可能完整的`Semaphore`:
1. 基础构造
    * `acquire` & `release`
    * `tryAcquire`
2. 支持一次获取/释放多个许可
3. 支持公平与非公平信号量
   * 公平信号量
   * 重构：委托模式
4. 优化：`synchronized` → CAS

## 1. 基础构造
由信号量的定义可知，它拥有一个整型成员变量`permit`：
```Java
public class MySemaphore {
    private int permits;
}
```
`permits`必须是自然数，负数没有意义。在构造器中必须加以限制：

```Java
public MySemaphore(int permits) {
    if (permits < 0) {
        throw new IllegalArgumentException("Permits cannot be negative.");
    }
    
    this.permits = permits;
}
```

### 1.1. `acquire` & `release`
接下来，我们需要实现获取与释放信号量的方法。这两个方法的逻辑与[`CountDownLatch`](https://blog.csdn.net/2503_91769056/article/details/147539618?spm=1001.2014.3001.5501)中的`await` & `countDown`非常相似:
* `acquire`表示获得一个许可，如果没有许可，则一直阻塞（等待）
* `release`表示释放一个许可，释放后通知正在阻塞的线程

```Java
public void acquire() throws InterruptedException {
    synchronized (this) {
        while (permits == 0) {
            this.wait();
        }
        
        permits--;
    }
}
```
注意，这里使用`while`循环来检查`permits`的值而不是`if`。这是因为如果不使用`while`来判断，会造成线程**虚假唤醒**的现象。

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
`release`方法比较简单：释放意味着将`permits`加1，并通知其他线程。
```Java
public void release() throws InterruptedException {
    synchronized (this) {
        permits++;
        this.notifyAll();
    }
}
```

### 1.2. `tryAcquire`
有时候我们希望没有获得许可的线程走另一种逻辑，于是我们需要一种能够立马判断是否获取成功的方法。这就是`tryAcquire`的功能：
* 如果能够立马获取成功，则获取一个许可并返回`true`
* 否则返回`false

```Java
public boolean tryAcquire() {
    synchronized (this) {
        if (permits > 0) {
            permits--;
            return true;
        } else {
            return false;
        }
    }
}
```
现在，我们写一个测试类来检查一下我们代码的行为。
1. 将信号量设置成100，这意味着有100个许可
2. 我们先让主线程尝试获得一次许可，预计是可以获得的
3. 之后让主线程创建100个子线程，每个线程获取一个信号量
4. 之后再让主线程获取一次许可，预计是不可以获得的
5. 等待，直到子线程完成工作释放许可，最后让主线程获取一次许可，预计是可以获得的
```Java
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
                    log.error(e.getMessage(), e);
                }
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
```
运行结果如下：
```aiignore
18:04:05.151 [main] INFO synchronizer.semaphore.TestMySemaphore -- Main thread trying to acquire permit...
18:04:05.155 [main] INFO synchronizer.semaphore.TestMySemaphore -- Result: true
18:04:06.190 [main] INFO synchronizer.semaphore.TestMySemaphore -- Main thread trying to release permit again...
18:04:06.190 [main] INFO synchronizer.semaphore.TestMySemaphore -- Result: false
18:04:16.193 [main] INFO synchronizer.semaphore.TestMySemaphore -- Main thread trying to release permit again...
18:04:16.193 [main] INFO synchronizer.semaphore.TestMySemaphore -- Result: true
```
测试结果与预期一致。

## 2. 支持一次获取/释放多个许可
回想一下我们之前的`acquire`逻辑：如果`permits == 0`，则当前线程阻塞；否则获取1个许可。

那么获取多个许可的`acquire`方法是相似的：如果当前线程不能获得指定数量的许可，则当前线程阻塞；否则获取多个许可。当然，获取0个许可或者负数个许可是没有意义的，我们必须检查这一点：
```Java
public void acquire(int n) throws InterruptedException{
    if (n <= 0) {
        throw new IllegalArgumentException("N cannot be negative");
    }
    
    synchronized (this) {
        while (permits < n) {
            this.wait();
        }
        
        permits -= n;
    }
}
```
实际上，最开始的`acquire`方法是这个方法`n == 1`的特例：
```Java
public void acquire() throws InterruptedException {
    this.acquire(1);
}
```

按照这个思路，最开始的`release`方法也是释放多个许可的`release`方法的一个特例:
```Java
public void release(int n) {
    if (n <= 0) {
        throw new IllegalArgumentException("N cannot be negative.");
    }
    
    synchronized (this) {
        permits += n;
        this.notifyAll();
    }
}

public void release() {
    this.release(1);
}
```
我们最后再来改进一下`tryAcquire`让它能够让线程立即尝试获取`n`个许可。思路与上面非常相似，不再赘述：
```Java
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

public boolean tryAcquire() {
    return this.tryAcquire(1);
}
```
那么，目前为止，一个最基本的`Semaphore`就完成了。目前为止的完整代码为：
```Java
public class MySemaphore {

    private int permits; // 许可数

    public MySemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Permits cannot be negative.");
        }

        this.permits = permits;
    }

    public void acquire() throws InterruptedException {
        this.acquire(1);
    }

    /**
     * 一次性获取n个许可
     * @param n n个许可
     * @throws InterruptedException 中断异常
     */
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
            if (permits >= n) {
                permits -= n;
                return true;
            }

            return false;
        }
    }
}
```
我们可以写一个测试来测试一下新加入的方法：
```Java
class TestMySemaphoreAlternate {
    public static void main(String[] args) throws InterruptedException {
        MySemaphore semA = new MySemaphore(3);
        semA.acquire(3); // permits = 0

        boolean secondA = semA.tryAcquire();
        Assertions.assertFalse(secondA); // 通过测试

        semA.release(2); // permits = 2

        boolean thirdA = semA.tryAcquire(); // permits = 1
        Assertions.assertTrue(thirdA); // 通过测试

        semA.acquire(2); // 1 < 2, 线程会被阻塞
    }
}
```
运行后，会发现行为与规定一致。

## 3. 支持公平与非公平信号量
### 3.1. 公平信号量
目前为止，我们的`Semaphore`是非公平的，新来的线程只要能够优先被调度就会取走许可。这与其他同步器具有一个相似的问题：容易出现线程饥饿问题。
为了解决这个问题，我们自然是希望按照“先来后到”的顺序让线程获得许可。那么最直接也是最好的方法就是维护一个**队列**：
```Java
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
}
```
这样，只要线程调用了`acquire` / `tryAcquire`方法，线程就会被加入队列中进行顺序等待。

一个值得注意的问题是，这个队列应该也要是线程安全的才行。那为什么我们可以先使用非线程安全的`ArrayDeque`呢？
这是因为目前在我们的方法逻辑中，我们都是使用`synchronized`块来实现的，所以只要与队列相关的逻辑都在`synchronized`块中，那么它就是线程安全的。

先来改造`acquire`方法。我们需要在原有方法的逻辑上增加：
1. 调用`acquire`方法的线程先进入队列末尾等待
2. 如果队首不是当前线程，或者许可不够，那么线程阻塞
3. 如果线程异常中断，或者线程满足拿走许可的条件，那么线程都要被从队列中移出

线程异常中断的处理容易被忽略，但这恰恰是最关键的：如果线程挂掉且没有被从队列中移出，那么在该线程后面的线程就会永远地阻塞。
```Java
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
```
那么`tryAcquire`的逻辑也是类似的，只不过线程不会被阻塞，而是立即返回一个`boolean`值：
1. 线程进入队列末尾
2. 如果不同时满足以下两个条件，则将自己从队列中移出并立即返回`false`
   * 队首不是自己
   * 许可不足
3. 如果满足了条件，则将自己从队列中移出、扣减许可，并理解返回`true`

```Java
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
```

那么到此，我们的方法已经是公平的了。但是，它们又不支持不公平信号量了。怎么办？

### 3.2. 重构：委托模式
点开官方的`Semaphore`源码，我们会发现源码中是这样构建`Semaphore`的：
```Java
// 未指定fair字段值，默认非公平
public Semaphore(int permits) {
        sync = new NonfairSync(permits);
}

public Semaphore(int permits, boolean fair) {
   sync = fair ? new FairSync(permits) : new NonfairSync(permits);
}
```
这里，官方使用了**委托模式**的设计模式。它的核心思想是，将某些功能请求，交给另一个辅助对象去完成，而不是自己直接实现。
简单来说，就是一个对象中“委托”/“调用”另一个对象的方法来完成工作。在这里，`Semaphore`的实现被指定给`FairSync`和`NonFairSync`两个对象。

为什么要这么做？在我们这个例子中，主要是简化主代码，降低耦合。如果我们不使用委托，那么我们就需要为我们所有的方法加上一个条件判断，才能使得方法同时支持公平与非公平信号量。
这不仅会导致代码的臃肿化，而且不利于后续的扩展。



