# 从零开始的`ConcurrentHashMap`

**Github: https://github.com/Emil-Stampfly-He/basics**

`ConcurrentHashMap`是线程安全版本的`HashMap`，其读操作不需要加锁，
而写操作则需要加锁来保证线程安全。这是因为，即使多线程并发地读取同一个key，
它们也不会修改数据结构，因此无锁访问时可能的。而对于写操作，多个线程同时进行
写操作可能导致数据不一致的竞态条件，因此需要上锁。

上锁的方式有很多，我们按照最大粒度的锁到最小粒度的锁的顺序讲解：
1. `synchronized`关键字
2. Segment锁 + `ReentrantLock`
3. bucket锁 + CAS

## `synchronized`关键字

如何给`ConcurrentHashMap`上锁？先来看一个最简单的版本：
整个`ConcurrentHashMap`就是一个巨大的`HashMap`，只不过它的写操作都使用了`synchronized`
关键字。
```Java
/**
 * 对整个HashMap上锁，锁的粒度是最粗的
 * 只有写操作需要上锁，读操作不需要上锁
 * @param <K> key
 * @param <V> value
 */
public class MyEasyConcurrentHashMap<K, V> {

    private final HashMap<K, V> hashMap = new HashMap<>();

    public synchronized void put(K key, V value) {
        hashMap.put(key, value);
    }

    public V get(K key) {
        return hashMap.get(key);
    }
}
```
这是一个非常简单的`ConcurrentHashMap`，我们仅仅只是对`put`方法整体上锁。让我们来验证一下是否正确：
```Java
@Slf4j
class TestMyEasyConcurrentHashMap {
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final String COUNT_KEY = "count_key";

    public static void main(String[] args) {
        for (int j = 0; j < 10000; j++) { // 反复检查10000次
            MyEasyConcurrentHashMap<String, AtomicInteger> easyMap = new MyEasyConcurrentHashMap<>();
            easyMap.put(COUNT_KEY, count);

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    easyMap.get(COUNT_KEY).addAndGet(1);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    easyMap.get(COUNT_KEY).addAndGet(1);
                }
            });

            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                log.error("Error", e);
            }

            log.info("Test done.");
            Assertions.assertEquals(20000, easyMap.get(COUNT_KEY).get());

            easyMap.get(COUNT_KEY).set(0);
        }

        log.info("10000 times of test done.");
    }
}
```
在测试类中，我们创建了两个子线程对`easyMap`中的数据反复地进行加1操作，每个线程操作10000次。如果这个`easyMap`
是线程安全的，那么最终的结果必然是20000。我们反复验证10000次，最终毫无问题。说明这个`easyMap`确实是线程安全的。

## Segment锁 + `ReentrantLock`
很显然，上面加上`synchronized`关键字互斥锁的方法非常低效，因为在写操作进行时，一个线程会获得整个`easyMap`的锁，
导致其他线程完全不能对其进行操作——锁的粒度非常大。有没有降低锁的粒度的方法？

一种非常自然的想法是，我们将整个`ConcurrentHashMap`拆成很多组线程安全的`HashMap`，每个组叫一个Segment（段）.
对于每个Segment中的`HashMap`，写操作加上`ReentrantLock`，读操作可以不上锁。
```Java
/**
     * 每一个Segment就是一个Map
     * 用多个小锁，代替一个大锁
     * ReentrantLock是一种显式锁，有上下文切换开销，是性能瓶颈所在
     * @param <K> key
     * @param <V> value
     */
    private static class Segment<K, V> {
        private final Map<K, V> map =  new HashMap<>();
        private final ReentrantLock lock = new ReentrantLock();

        public void put(K key, V value) {
            lock.lock();
            try {
                map.put(key, value);
            } finally {
                lock.unlock();
            }
        }

        public V get(K key) {
            return map.get(key);
        }
    }
```
当然，在这个简易版本中的`ConcurrentHashMap`中，直接在`put`方法上加上`synchronized`关键字与`ReentrantLock`
没有太大的区别，都是显式锁的一种。只不过`ReentrantLock`提供了一些其他更灵活的方法，使得锁的粒度不仅仅是方法级的。

我们修改一下上面的测试类，同样重复测试10000次，能够得到最终结果仍然是20000.改进版的ConcurrentHashMap仍然是
线程安全的。

## bucket锁 + CAS
实际上，第二个版本的`ConcurrentHashMap`的设计原理接近于Java中的早期实现。但在Java 8之后的`ConcurrentHashMap`
对设计上做出了进一步的改进，使用了bucket数组和CAS操作来实现更高效的并发控制。

尽管我们已经使用立刻Segment数组将锁的粒度分到了多个`HashMap`上，但是每个Segment还是使用了同一个全局锁。
我们自然可以想到一个更加降低锁粒度的方法：给每个bucket上锁，这样就避免了一个大的锁对性能的限制。

另一个影响性能的因素是前面已经提到的“显式锁”。当一个线程获得锁时，其他线程必须等待（阻塞），直到锁被释放。
而CAS作为乐观锁，其优点是：在保证操作是原子性的同时，它不需要加锁，避免了线程间的阻塞和上下文切换。因此，
CAS操作的性能优于显式锁。

我们可以进行进一步的改进：
```Java
/**
 * 对每个bucket上锁，锁的粒度最细
 * 同时使用CAS操作，避免了显示锁操作，提升了性能
 * @param <K> key
 * @param <V> value
 */
public class MyHardConcurrentHashMap<K, V> {

    private static final int INITIAL_CAPACITY = 16;
    private final AtomicReferenceArray<Node<K, V>> buckets;

    public MyHardConcurrentHashMap() {
        this.buckets = new AtomicReferenceArray<>(INITIAL_CAPACITY);
    }

    private int getBucketIndex(K key) {
        return (key.hashCode() & 0x7FFFFFFF) % INITIAL_CAPACITY;
    }

    /**
     * 避免了ReentrantLock显式锁
     * @param key key
     * @param value value
     */
    public void put(K key, V value) {
        int index = getBucketIndex(key);
        Node<K, V> newNode = new Node<>(key, value);

        while (true) {
            Node<K, V> current = buckets.get(index);
            newNode.next = current;

            // CAS操作
            // index: buckets[index]
            // expectedValue: current
            // newValue: newNode
            // 如果再buckets[index]中，期望值与实际值不匹配，
            // 说明有其他线程修改了，操作失败，重试，直到成功为止
            if (buckets.compareAndSet(index, current, newNode)) {
                // compare: buckets[index] =?= current
                // set: current -> newNode
                break;
            }
        }
    }

    public V get(K key) {
        int index = getBucketIndex(key);
        Node<K, V> current = buckets.get(index);

        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }

            current = current.next;
        }

        return null;
    }

    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K k, V v) {
            this.key = k;
            this.value = v;
        }
    }
}
```
这里有两点需要说明：
* `AtomicReferenceArray`

`AtomicReferenceArray`中的每个元素都能进行原子操作，相当于一个线程安全的数组。
* `compareAndSet`

`compareAndSet`是`AtomicReferenceArray`类中的一个方法，也是CAS操作的实现。
这个操作的目的是原子性地更新数组中的某个位置。

再次对这个版本的`ConcurrentHashMap`进行测试，仍然可以全部通过。

## 后续的改进
官方的`ConcurrentHashMap`是结合了红黑树实现的。当哈希桶中的元素增多时，红黑树能够将查找实现复杂度
从$O(n)$降低到$O(log\ n)$。具体来说，当链表元素数量超过8时，链表就会被树化。

当然，官方版本的`put`操作同时使用了CAS与`synchronized`关键字（混合锁）：
```Java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // ...
    
    // CAS
    if (casTabAt(tab, i, null, new Node<K, V>(hash, key, value)))
        break; // no lock when adding to empty bin

    // synchronized
    synchronized (f) {
        if (tabAt(tab, i) == f)
            //...
    }
    
    //...
    
    return null;
}
            
```
两者是互补的：CAS用于简单的无冲突更新，加锁则用于复杂的结构修改。
在桶为空时，通过CAS操作尝试将新节点插入，不需要加锁也能确保当桶为空时可以安全插入节点。
当桶不为空时，会在当前节点`f`上加锁（`synchronized (f)`）。这确保了在修改这个桶中的节点时，只有一个线程能够访问。
由于树的操作非常复杂，使用`synchronized`关键字会是一种非常直观的方式，完全避免了竞态条件的产生。