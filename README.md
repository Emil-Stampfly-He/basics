# 从零开始手写所有八股文（持续更新）

![Lines of Code](https://img.shields.io/endpoint?url=https://Emil-Stampfly-He.github.io/basics/badge.json)

## 一、Java核心语言特性
### 1. 基本类型与包装类型
* 自动装箱/拆箱陷阱
* `Integer.valueOf`缓存机制
### 2. 泛型
* 类型擦除机制
### 3. 反射 & 动态代理
* `Class.forName()` / `newInstance()`
* JDK动态代理 vs CGLIB代理原理
  * [JDK动态代理](Core/src/main/java/proxy/jdk/JDKDynamicProxy.md)
  * [CGLIB动态代理](Core/src/main/java/proxy/cglib/CGLIBDynamicProxy.md)
### 4. 注解处理
* Lombok的`@Data`

## 二. 并发组件
### 1.集合类
* `BlockingQueue`
  * [`ArrayBlockingQueue`](Concurrent/src/main/java/sets/blocking_queue/ArrayBlockingQueue.md)
  * `LinkedBlockingQueue`
* [`ConcurrentHashMap`](Concurrent/src/main/java/sets/concurrent_hash_map/ConcurrentHashMap.md)
* `CopyOnWriteArray`
* [线程池 `ThreadPoolExecutor`](Concurrent/src/main/java/sets/thread_pool/ThreadPoolExecutor.md)
### 2. 同步器
* [`CountDownLatch`](Concurrent/src/main/java/synchronizer/count_down_latch/CountDownLatch.md)
* [`Semaphore`](Concurrent/src/main/java/synchronizer/semaphore/Semaphore.md)
### 3. 锁机制
* `ReentrantLock`、`StampedLock`、`ReadWriteLock`
* 自旋锁
* CLH/FLAT 队列锁

## 三、JVM & 性能调优
### 1. 类加载子系统
* 双亲委派模型
* 自定义`ClassLoader`
### 2. 内存模型 & 垃圾回收
* 堆/栈/方法区布局
* 标记-清除／标记-整理／分代收集
### 3. JVM参数调优
* `Xmx`、`Xms`、GC日志解析

## 四、框架
### 1. Spring
#### 1.1 AOP
* 动态代理
  * [JDK动态代理](Core/src/main/java/proxy/jdk/JDKDynamicProxy.md)
  * [CGLIB动态代理](Core/src/main/java/proxy/cglib/CGLIBDynamicProxy.md)
  * [Spring选择代理](Core/src/main/java/proxy/selective_proxy/SpringSelectiveProxy.md)