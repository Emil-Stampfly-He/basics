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
4. 优化？`synchronized` → CAS
   * `VarHandle`实现CAS操作
   * CAS性能一定比`synchronized`好吗？

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

接下来，我们创建一个`Sync`类作为委托父类，之后再创建两个委托子类用于具体实现公平与非公平方法：`NonfairSync` & `FairSync`。这个父类默认实现的是非公平信号量：
```Java
public class MySemaphore {

   private final Sync sync; // 委托类成员变量

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

      @SuppressWarnings("unused")
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
}
```
注意到我们将原来`MySemaphore`的成员变量`permits`也移交给了`Sync`进行委托，这样做是为了最大程度上地解除耦合。

接下来就是`NonfairSync` & `FairSync`。对于`NonfairSync`，由于其父类默认实现的是非公平，所以它不需要重载任何方法。而`FairSync`则需要重载`acquire`和`tryAcquire`方法，使得它们是公平的。
我们已经在上一小节实现过了公平的`acquire`和`tryAcquire`，所以只需要复制粘贴即可（可以加一个`@Override`提醒自己）：
```Java
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
```
到此为止，委托类就完全实现好了。此时我们要做的就是实现暴露给调用者的方法：构造器与具体方法。
1. 对于构造器，我们允许用户传入或不传入一个`boolean`值，从而视情况指定公平还是非公平。
```Java
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
```
可见，我们表面上调用的是`MySemaphore`，实际上内部是创建了一个新的`Sync`子类对象。
2. 对于其他方法，只需要指定成员变量`sync`调用即可。

```Java
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
```
当调用上述的方法时，`sync`会自动根据自己的在构造器中被指定的具体类型，选取公平还是非公平的方法。

我们的重构到此就结束了，也离官方的实现又更近了一步。当然，官方的`Sync`继承了`AbstractQueuedSynchronizer`（AQS，队列同步器；被用来实现各种锁以及各种同步工具），因此会有更多可以直接使用的方法，我们在此做了简化处理。目前为止的代码如下：

```Java
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
```

## 4. 优化？`synchronized` → CAS
在并发编程中，我们最直观的做法是用 `synchronized` 加 `wait`/`notify` 来实现信号量——线程许可不足时进入阻塞，许可到来时用 notifyAll() 唤醒。但阻塞和唤醒背后其实走的是操作系统的 `park`/`unpark`，往往要付出一次完整的上下文切换成本。
既然阻塞有开销，能不能让线程在不拿到许可的时候直接自旋呢？这就是CAS操作。我们希望的是，如果线程没法拿到足够的许可，就自旋等待；否则就原子地更新。

如何做到原子更新？点开`Semaphore`源码，可以看到它使用了一个叫`compareAndSetState`的方法。这个方法是一个本地方法（`native`方法），属于AQS，`State`就是我们所说的“许可”。而这个方法又是由`Unsafe`类中的`compareAndSetInt`实现的。
但是，JDK的`Unsafe`类并没有提供公开的API，这说明官方并不希望我们直接使用`Unsafe`类，而且我们也不可能再去写一遍底层代码。那有什么替代方案吗？

### 4.1. `VarHandle`
其实不只是JDK有一个`UnSafe`类，`sun.misc`包下也有一个`Unsafe`类，甚至是公开API可供我们使用：

```Java
import sun.misc.Unsafe;

private static final Unsafe U = Unsafe.getUnsafe();
```
尝试调用一下它里面的方法，却发现几乎全部被加上了`@Deprecated`标识，且未来会被移除，不能保证跨版本兼容，使用的话甚至会报错。好在注释说明了，我们应该转而使用`VarHandle`来实现相同的功能。

简而言之，`VarHandle`是用于替代`Unsafe`的，暴露安全可控的、跨版本兼容的底层变量访问手段，而且比原子类更加通用。所以，我们转而在`MySemaphore`中创建一个`VarHandle`的对象：
```Java
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
```
这样，我们就可以在`Sync`及其子类中使用`VarHandle`给我们提供的底层指令了：
1. 首先判断许可是否充足，不足则自旋等待
2. 如果充足，原子地更新许可数
```Java
public void acquire(int n) throws InterruptedException {
   if (n <= 0) {
       throw new IllegalArgumentException("N must be greater than zero.");
   }

//  synchronized (this) {
//      while (permits < n) {
//      this.wait();
//      }
//
//      permits -= n;
//  }

   // CAS
   while (true) {
       int available = this.permits;
       int remaining = available - n;
       if (remaining < 0) {
           // 许可暂时不够，自旋等待
           Thread.onSpinWait();
           continue;
       }

       if (V.compareAndSet(this, available, remaining)) {
           break;
       }
   }
}
```
需要注意的是，之所以将CAS方法放在死循环中，是因为CAS方法不一定一次尝试就能成功。所以需要给它多次尝试更新的机会，直到更新成功为止。

按照类似的逻辑，我们也可以更改一下`tryAcquire`方法：
```Java
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

   // CAS
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
```
逻辑与`acquire`是一样的，只不过调用这个方法的线程不满足条件不会被阻塞，而是直接返回`false`。

既然`acquire`和`tryAcquire`都不再使用`wait`来唤醒线程，`release`方法也需要去掉`notify`/`notifyAll`方法：
```Java
public void release(int n) {
    if (n <= 0) throw new IllegalArgumentException("N cannot be negative");
    
    // 无条件地加上，因此不需要放到死循环中
    V.getAndAdd(this, n);
}
```
因为释放许可是无条件的，可以直接释放，所以`release`的CAS操作不需要放到死循环中。

### 4.2. CAS性能一定比`synchronized`好吗？
以上，我们将全部的`synchronized`块改成了CAS。很多人的第一反应是，CAS性能肯定更好了。事实情况真的是这样吗？

当我们在比较CAS与`synchronized`的时候，我们不仅仅是比较CAS与`synchronized`关键字本身，而是比较CAS与JVM内部用于实现`synchronized`的算法的性能。
`synchronized`关键字底层本身是自旋+挂起(`park`)的结合：JVM的`synchronized`在失败后，会先做出有限次的自旋，再滑向挂起，让出CPU。而在我们的CAS操作中：
```Java
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
```
线程如果拿不到许可，就会一直在死循环中自旋，不断消耗CPU资源。如果并发线程数超过CPU核心数，线程们就会抢着自旋，自旋尝试不过也不会被挂起，导致大量的“空转”，严重拉低了整体的吞吐。
所以，虽然`synchronized`中线程面临上下文切换的代价，但却能够在高并发场景下有效避免忙等。

此外，`synchronized`本身借鉴了CAS+自旋的思想。一个对象监视器(monitor)的内部信息主要包括：一个表示锁状态及重入次数的`int`值、一个指向当前持有锁的线程的引用，以及一个等待队列（queue），存放所有等待获取该监视器的线程。
等待队列的操作最为耗时：将线程加入队列、将其从线程调度中移除，以及在当前持有者释放锁时将其唤醒，都会耗费相当的时间。

在无竞争的情况下，等待队列自然不会参与。获取监视器仅需一次 CAS 操作，将状态从“未占用”（通常为 0）更新为“已占用、重入次数为 1”（某个常见值），成功后线程即可进入临界区；释放监视器时只需写回“未占用”状态（同时保证必要的内存可见性），然后如果有挂起的线程就唤醒其中一个。
这便是`synchronized`的**轻量级锁**实现。

如果不仅仅是无竞争，而且同一个线程在首次尝试执行`synchronized`块后仍然没有其他线程进入块，那么`synchronized`会被优化成**偏向锁**。如果以后同一线程再来，可以直接进入临界区，实现零CAS（可以理解为**偏向**这个线程）。

因此，从本质上说，原有的`synchronized`块方法反而更接近官方的实现。官方的实现采用了自旋与挂起的结合：
```Java
final int acquire(Node node, int arg, boolean shared,
                  boolean interruptible, boolean timed, long time) {
   Thread current = Thread.currentThread();
   byte spins = 0, postSpins = 0;   // retries upon unpark of first thread
   boolean interrupted = false, first = false;
   Node pred = null;               // predecessor of node when enqueued
   
   for (;;) {
      if (!first && (pred = (node == null) ? null : node.prev) != null &&
              !(first = (head == pred))) {
         if (pred.status < 0) {
            cleanQueue();           // predecessor cancelled
            continue;
         } else if (pred.prev == null) {
            Thread.onSpinWait();    // ensure serialization
            continue;
         }
      }
      if (first || pred == null) {
         boolean acquired;
         try {
            if (shared)
               acquired = (tryAcquireShared(arg) >= 0);
            else
               acquired = tryAcquire(arg);
         } catch (Throwable ex) {
            cancelAcquire(node, interrupted, false);
            throw ex;
         }
         if (acquired) {
            if (first) {
               node.prev = null;
               head = node;
               pred.next = null;
               node.waiter = null;
               if (shared)
                  signalNextIfShared(node);
               if (interrupted)
                  current.interrupt();
            }
            return 1;
         }
      }
      Node t;
      if ((t = tail) == null) {           // initialize queue
         if (tryInitializeHead() == null)
            return acquireOnOOME(shared, arg);
      } else if (node == null) {          // allocate; retry before enqueue
         try {
            node = (shared) ? new SharedNode() : new ExclusiveNode();
         } catch (OutOfMemoryError oome) {
            return acquireOnOOME(shared, arg);
         }
      } else if (pred == null) {          // try to enqueue
         node.waiter = current;
         node.setPrevRelaxed(t);         // avoid unnecessary fence
         if (!casTail(t, node))
            node.setPrevRelaxed(null);  // back out
         else
            t.next = node;
      } else if (first && spins != 0) {
         --spins;                        // reduce unfairness on rewaits
         Thread.onSpinWait();
      } else if (node.status == 0) {
         node.status = WAITING;          // enable signal and recheck
      } else {
         spins = postSpins = (byte)((postSpins << 1) | 1);
         try {
            long nanos;
            if (!timed)
               LockSupport.park(this);
            else if ((nanos = time - System.nanoTime()) > 0L)
               LockSupport.parkNanos(this, nanos);
            else
               break;
         } catch (Error | RuntimeException ex) {
            cancelAcquire(node, interrupted, interruptible); // cancel & rethrow
            throw ex;
         }
         node.clearStatus();
         if ((interrupted |= Thread.interrupted()) && interruptible)
            break;
      }
   }
   return cancelAcquire(node, interrupted, interruptible);
}
```
方法逻辑复杂不看。我们只需注意到方法中混合使用了`onSpinWait`和`park`方法即可。