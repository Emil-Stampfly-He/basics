package concurrent_hash_map;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

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

@Slf4j
class TestMyHardConcurrentHashMap {

    private static final AtomicInteger count = new AtomicInteger(0);
    private static final String COUNT_KEY = "count_key";

    public static void main(String[] args) {
        MyHardConcurrentHashMap<String, AtomicInteger> hardMap = new MyHardConcurrentHashMap<>();
        for (int j = 0; j < 10000; j++) {
            hardMap.put(COUNT_KEY, count);

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    hardMap.get(COUNT_KEY).incrementAndGet();
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    hardMap.get(COUNT_KEY).incrementAndGet();
                }
            });

            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }

            log.info("Test done.");
            Assertions.assertEquals(20000, hardMap.get(COUNT_KEY).get());

            hardMap.get(COUNT_KEY).set(0);
        }

        log.info("100 times of tests done.");
    }
}
