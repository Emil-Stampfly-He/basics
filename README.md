# 学习笔记 `TODO List`

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

## 四、OOP设计模式：Rust vs Java
### 1. 行为模式
* [策略模式](OOPDesign/src/main/rust/src/strategy/strategy.md)
* [命令模式](OOPDesign/src/main/rust/src/command/command.md)
* [观察者模式](OOPDesign/src/main/rust/src/observer/observer.md)
### 2. 结构型模式
* [装饰者模式](OOPDesign/src/main/rust/src/decorator/decorator.md)
### 3. 创建型模式
* [工厂模式](OOPDesign/src/main/rust/src/factory/factory.md)

## 五、框架及中间件
### 1. Spring Framework
1. Bean
   * 容器
     * [容器接口](Frameworks/src/main/java/spring/bean/ContextInterface.md)
     * [容器实现](Frameworks/src/main/java/spring/bean/ContextImplementation.md)
   * 后处理器
     * [Bean后处理器](Frameworks/src/main/java/spring/bean/bean_post_processor/BeanPostProcessor.md)
     * [详解`@Autowired`的Bean后处理器](Frameworks/src/main/java/spring/bean/bean_post_processor/DigInAutowired.md)
     * [Bean工厂后处理器](Frameworks/src/main/java/spring/bean/bean_factory_post_processor/BeanFactoryPostProcessor.md)
   * [`Aware`和`InitializingBean`](Frameworks/src/main/java/spring/bean/aware/AwareAndInitializingBean.md)
   * [初始化与销毁](Frameworks/src/main/java/spring/bean/init_destroy/InitAndDestroy.md)
   * [Scope]()
2. AOP
   * 动态代理
     * [JDK动态代理](Core/src/main/java/proxy/jdk/JDKDynamicProxy.md)
     * [CGLIB动态代理](Core/src/main/java/proxy/cglib/CGLIBDynamicProxy.md)
     * [Spring选择代理](Core/src/main/java/proxy/selective_proxy/SpringSelectiveProxy.md)
   * [切点匹配](Frameworks/src/main/java/spring/aop/PointcutMatching.md)
   * [高级切面与低级切面](Frameworks/src/main/java/spring/aop/AdvisorAndAspect.md)
   * 通知调用
     * [静态通知调用](Frameworks/src/main/java/spring/aop/StaticInvocationOfAdvice.md)
     * [动态通知调用](Frameworks/src/main/java/spring/aop/DynamicInvocationOfAdvice.md)
   * [Spring AOP总结](Frameworks/src/main/java/spring/aop/summarization/SpringAOPSummarization.md)
### 2. Redis