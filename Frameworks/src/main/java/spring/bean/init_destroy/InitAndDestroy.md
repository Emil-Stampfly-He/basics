# 初始化与销毁

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

一般使用的初始化/销毁方式分别都有3种：
1. 基于注解的扩展功能（`@PostConstruct`和`@PreDestroy`）
2. 内置功能（`InitializingBean`和`DisposableBean`接口）
3. `BeanDefinition`中定义的初始化/销毁方法

假设我们有一个启动类和两个Bean，`Bean1`实现了3种初始化方法，而`Bean2`实现了3种销毁方法：
```java
@SpringBootApplication
public class InitAndDestroy {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(InitAndDestroy.class, args);
        context.close();
    }
    
    // 在BeanDefinition中定义初始化与销毁方法
    @Bean(initMethod = "init3") public Bean1 bean1() {return new Bean1();}
    @Bean(destroyMethod = "destroy3") public Bean2 bean2() {return new Bean2();}
}
```
```java

@Slf4j
public class Bean1 implements InitializingBean {
    @PostConstruct
    public void init1() {log.info("init 1");}
    @Override
    public void afterPropertiesSet() throws Exception {log.info("init 2");}
    public void init3() {log.info("init 3");}
}
```
```java
@Slf4j
public class Bean2 implements DisposableBean {
    @PreDestroy
    public void destroy1() {log.info("destroy1");}
    @Override
    public void destroy() throws Exception {log.info("destroy2");}
    public void destroy3() {log.info("destroy3");}
}
```
注意：`DisposableBean`与`Aware`和`InitializingBean`一样，是内置功能，专门针对Bean的销毁。

虽然工作中我们不可能同时使用多种方式来初始化Bean，但是作为研究目的，我们打印一下看看方法调用的顺序：
```aiignore
2025-05-20T20:13:16.819+01:00  INFO 3128 --- [           main] spring.bean.init_destroy.Bean1           : init 1
2025-05-20T20:13:16.819+01:00  INFO 3128 --- [           main] spring.bean.init_destroy.Bean1           : init 2
2025-05-20T20:13:16.819+01:00  INFO 3128 --- [           main] spring.bean.init_destroy.Bean1           : init 3

2025-05-20T20:19:06.579+01:00  INFO 10032 --- [           main] spring.bean.init_destroy.Bean2           : destroy1
2025-05-20T20:19:06.580+01:00  INFO 10032 --- [           main] spring.bean.init_destroy.Bean2           : destroy2
2025-05-20T20:19:06.580+01:00  INFO 10032 --- [           main] spring.bean.init_destroy.Bean2           : destroy3
```
可以看到，`@PostConstruct`这个扩展功能中定义的初始化方法被最先执行，而`BeanDefinition`中的初始化方法最后执行。类似地，`@PreDestroy`这个扩展的销毁方法先被执行，然后是内置方法，最后是`BeanDefinition`中定义的方法。