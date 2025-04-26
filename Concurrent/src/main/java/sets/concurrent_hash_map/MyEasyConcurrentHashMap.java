package sets.concurrent_hash_map;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
