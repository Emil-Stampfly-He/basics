package sets.concurrent_hash_map;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 对每一个Segment上锁，也就是将一个大的HashMap分成多次上锁，锁的粒度降低
 * 不同的key可以同时写，因为它们不相关
 * @param <K> key
 * @param <V> value
 */
public class MyConcurrentHashMap<K, V> {

    private static final int SEGMENT_COUNT = 16;
    private final Segment<K, V>[] segments;

    @SuppressWarnings("unchecked")
    public MyConcurrentHashMap() {
        segments = new Segment[SEGMENT_COUNT];
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            segments[i] = new Segment<>();
        }
    }

    /**
     * 分段方法，取哈希值，然后对段的数量取模
     * @param key key
     * @return 段的index
     */
    private int getSegmentIndex(K key) {
        return (key.hashCode() & 0x7FFFFFFF) % SEGMENT_COUNT;
    }

    public void put(K key, V value) {
        int index = getSegmentIndex(key);
        segments[index].put(key, value);
    }

    public V get(K key) {
        int index = getSegmentIndex(key);
        return segments[index].get(key);
    }

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
}

@Slf4j
class TestMyConcurrentHashMap {
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final String COUNT_KEY = "count_key";

    public static void main(String[] args) {
        for (int j = 0; j < 10000; j++) { // 反复检查10000次
            MyConcurrentHashMap<String, AtomicInteger> map = new MyConcurrentHashMap<>();
            map.put(COUNT_KEY, count);

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    map.get(COUNT_KEY).addAndGet(1);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 10000; i++) {
                    map.get(COUNT_KEY).addAndGet(1);
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
            Assertions.assertEquals(20000, map.get(COUNT_KEY).get());

            map.get(COUNT_KEY).set(0);
        }

        log.info("100 times of test done.");
    }
}