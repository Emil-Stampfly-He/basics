# Spring AOP总结
我们以日常编写AOP进行方法增强的代码为例来整体梳理AOP背后到底发生了什么。我们需要：
1. 配置类：开启代理
2. 切面类：这里准备了一个前置通知和一个环绕通知
3. 目标类
4. 启动类
```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true) // 将会使用CGLIB代理
@ComponentScan(basePackages = "spring.aop.summarization")
public class AopConfig {}
```
```java
@Slf4j
@Aspect
@Component
public class MyAspect {
    // 前置增强foo方法
    @Before("execution(* spring.aop.summarization.Target.foo(..))")
    public void beforeAdvice(JoinPoint joinPoint) {
        log.info("Foo is ready to do something...");
    }

    // 环绕增强bar方法
    @Around("execution(* spring.aop.summarization.Target.bar(..))")
    public Object executionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Bar is ready to do something...");
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        log.info("Bar execution time: {}", end - start);
        return result;
    }
}
```
```java
@Component
@Slf4j
public class Target {
    public void foo() {fooDoSomething();}
    public void bar() {barDoSomething();}

    public void fooDoSomething() {try {Thread.sleep(1000);} catch (InterruptedException e) {throw new RuntimeException(e);}}
    public void barDoSomething() {try {Thread.sleep(1000);} catch (InterruptedException e) {throw new RuntimeException(e);}}
}
```
```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(MyApplication.class, args);
        Target target = (Target) context.getBean("target"); // 获得代理对象
        target.foo(); // 代理对象将调用增强了的方法
        target.bar();
    }
}
```
我们运行启动类，可以看到预期的输出结果：
* `foo`方法被前置增强
* `bar`方法被环绕增强
```aiignore
2025-05-08T11:16:12.382+01:00  INFO 13260 --- [           main] spring.aop.summarization.MyAspect        : Foo is ready to do something...
2025-05-08T11:16:13.384+01:00  INFO 13260 --- [           main] spring.aop.summarization.MyAspect        : Bar is ready to do something...
2025-05-08T11:16:14.386+01:00  INFO 13260 --- [           main] spring.aop.summarization.MyAspect        : Bar execution time: 1002
```
接下来，我们主要关注Spring AOP中发生的事情。

1. **解析切面定义**  
   Spring 启动时扫描所有 `@Aspect` 注解的类（如 `MyAspect`），并把带有 `@Before`、`@Around`、`@After` 等方法标记的元信息提取出来，交给一个 **AspectJAnnotationAdvisorFactory** 去生成“候选” `Advisor`。

2. **组装低级 Advisor**  
   `AspectJAnnotationAdvisorFactory` 会将每个切面方法分装为一个 `Pointcut` + `Advice`：  
   1. 对于 `@Before("…")`，生成一个 `AspectJExpressionPointcut` 和一个 `AspectJMethodBeforeAdvice`，再封装成一个 `DefaultPointcutAdvisor`；  
   2. 对于 `@Around("…")`，生成一个 `AspectJExpressionPointcut` 和一个 `AspectJAroundAdvice`，再封装成另一个 `DefaultPointcutAdvisor`；  
   3. 所有这些 `Advisor` 都被当成候选切面缓存下来，等待后续匹配。  
   4. 同时，Spring 也注册了一个关键的 `BeanPostProcessor`：**`AnnotationAwareAspectJAutoProxyCreator`**。
      1. `AnnotationAwareAspectJAutoProxyCreator`后续会根据切面类型自动生成代理对象

3. **容器刷新 & `BeanPostProcessor` 注册**  
   当 `SpringApplication.run(...)` 完成扫描和注册后，容器会把 `AnnotationAwareAspectJAutoProxyCreator` 等所有 `BeanPostProcessor` 都注册到生命周期中。

4. **Bean 实例化与初始化**（省略细节）  
   对每一个普通 Bean（比如：`Target`），
   1. Spring会用构造器创建实例并进行依赖注入；
      * 如果Bean没有循环依赖，那么`AnnotationAwareAspectJAutoProxyCreator`创建代理对象的时机在Bean初始化后
      * 如果Bean被检测到有循环依赖，那么`AnnotationAwareAspectJAutoProxyCreator`创建代理对象的时机在依赖注入前
   2. 进行Bean的后处理；
   3. 进入AOP代理逻辑；

5. **自动代理 (`wrapIfNecessary`)**  
   在Bean的后处理中：  
   1. `AnnotationAwareAspectJAutoProxyCreator#wrapIfNecessary`判断
      * 这个类是不是基础设施（切面本身、Advisor、Advice 等）如果是，跳过；否则执行`findEligibleAdvisors()`
   2. 调用 `findEligibleAdvisors()`：它会拿之前缓存的所有候选 `Advisor`，对每一个执行**静态匹配**，找到所有匹配当前 Bean 的切面；  
   3. 若结果不为空，就新建一个 `ProxyFactory`，决定用 JDK 代理还是 CGLIB，给它设置目标对象和接口/类信息； 
      * `proxyTargetClass = false` && 目标实现接口：JDK
      * `proxyTargetClass = false` && 目标未实现接口：CGLIB
      * `proxyTargetClass = true`：CGLIB
   4. 把上一步过滤出的 `Advisor` 全部加到 `ProxyFactory`；  
   5. 调用 `proxyFactory.getProxy()`，生成代理对象，并替换掉容器中原来的 Bean 引用。

6. **代理对象注入**  
   以后别的 Bean 依赖 `Target` 时，Spring 注入的都是上面生成的代理（`JdkDynamicAopProxy` 或 `ObjenesisCglibDynamicAopProxy`），而不是直接的 `new Target()`。

7. **运行时方法调用 & 拦截链执行**  
   当调用 `target.foo()` 或 `target.bar()`时：  
   1. 代理对象拦截调用，进入 `JdkDynamicAopProxy#invoke`或 `CglibAopProxy#intercept`；
      1. 先执行`AdvisedSupport#getInterceptorsAndDynamicInterceptionAdvice()`
         * 将所有通知统一转换为静态通知
         * 或者拿到动态通知，但我们这个例子中没有动态通知
      2. 再先根据 `Advisor` 列表为该方法构建 `MethodInterceptor` 链
         * `ExposeInvocationInterceptor`会创建一个最外围的`ADVISOR`切面
         * 同时会将调用链存入自己的`ThreadLocal`中
   2. 调用 `ReflectiveMethodInvocation.proceed()`，沿链执行（递归）：  
      - **Before Advice**（`@Before`）── 执行 `beforeAdvice()`；  
      - **Around Advice**（`@Around`）── 调用 `proceed()` 前逻辑；  
      - **目标方法**（`Target#foo` / `bar`）实际上被调用；  
      - 开始一层层退出，**Around Advice**后逻辑被执行
      - 退出**Before Advice**
   3. 链条执行完毕，最终结果返回给调用者。
