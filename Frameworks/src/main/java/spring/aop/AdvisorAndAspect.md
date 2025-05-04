# 高级切面与低级切面（`@Aspect` vs Advisor）

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

## 1. 回顾：`@Aspect`与advisor
我们先准备好一个高级切面和一个低级切面。一个高级切面用`@Aspect`注解表示；一个低级切面由一个pointcut和一个advice组成：
```java
public class AdvisorAndAspect {
    static class Target1 {public void foo() {System.out.println("target1 foo");}}
    static class Target2 { public void bar() {System.out.println("target2 bar");}}

    @Aspect // 高级切面（aspect）
    static class Aspect1 {
        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }

        @After("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }

    @Configuration // 低级切面（advisor），需要使用@Configuraion注解
    static class Config {
        @Bean // advice3需要作为bean传入
        public Advisor advisor3(MethodInterceptor advice3) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* foo());");
            return new DefaultPointcutAdvisor(pointcut, advice3);
        }

        @Bean
        public MethodInterceptor advice3() {
            return invocation -> {
                System.out.println("advice3 before... ");
                Object result = invocation.proceed();
                System.out.println("advice3 after...");
                return result;
            };
        }
    }
}
```
然后我们手动创建一个bean容器并将这些bean放到这个容器中：

```java
public class AdvisorAndAspect {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext(); // bean容器
        context.registerBean("aspect1", Aspect1.class);
        context.registerBean("config", Config.class);
        context.registerBean(ConfigurationClassPostProcessor.class); // 
        context.refresh();
        
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
    }
    /*...*/
}
```
输出结果如下：
```aiignore
aspect1
config
org.springframework.context.annotation.ConfigurationClassPostProcessor
advisor3
advice3
```
第一行
可以看到，容器不仅将低级切面`Config`注册成了一个bean，还将低级切面拆开，分别解析出了advisor和通知的bean。

## 2. `AnnotationAwareAspectJAutoProxyCreator`

### 2.1. 简介
每当使用切面时，我们从来不需要关心代理对象时怎么来的，因为Spring帮我们已经创建好了。那么“黑盒子”底下到底是谁在发挥作用？
主角便是`AnnotationAwareAspectJAutoProxyCreator`。`AnnotationAwareAspectJAutoProxyCreator`是一个bean后处理器（它实现了`BeanPostProcessor`接口），
它可以找到容器中所有的切面，包括高级与低级切面。找到后，如果是高级切面，则会将高级切面转换成低级切面。最后根据这些切面创建代理对象。这两点功能也可以从它的名字中解读出来：
`AnnotationAware` + `AutoProxyCreator`。

`AnnotationAwareAspectJAutoProxyCreator`自己本身作为一个bean，会在两个时间点发挥作用：依赖注入前与初始化后（*为发挥作用时间点）：
```aiignore
Bean的生命周期：
创建 ->（*）依赖注入 -> 初始化（*）-> ...
```

`AnnotationAwareAspectJAutoProxyCreator`继承了两个非常重要的方法：
1. `findEligibleAdvisors()` \
该方法继承自`AbstractAdvisorAutoProxyCreator`，用于寻找“有资格”的低级切面（advisors）并将它们加入到一个集合中。高级切面会先被解析成低级切面（advisor）之后加入其中。
2. `wrapIfNecessary()` \
该方法继承自`AbstractAutoProxyCreator`。如果有必要，则为这个bean创建代理（也就是wrap的意思）。那么怎样才是“有必要”呢？
   * 目标类确实有匹配的切面
   * 如果目标类根本没有匹配的切面，则不创建代理

### 2.2. `findEligibleAdvisors()`
由于该方法都是`protected`，所以下面我们使用反射调用：
```java
public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("aspect1", Aspect1.class);
    context.registerBean("config", Config.class);
    context.registerBean(ConfigurationClassPostProcessor.class);

    // 能识别高级切面中的注解，例如@Aspect，@Before
    // 能根据收集的切面自动创建代理
    context.registerBean(AnnotationAwareAspectJAutoProxyCreator.class);
    // AnnotationAwareAspectJAutoProxyCreator实现了BeanPostProcessor接口
    // 创建 ->（*）依赖注入 -> 初始化（*）

    context.refresh();
    for (String name : context.getBeanDefinitionNames()) {
        System.out.println(name);
    }

    System.out.println();

    /* findEligibleAdvisors() */
    AnnotationAwareAspectJAutoProxyCreator creator = context.getBean(AnnotationAwareAspectJAutoProxyCreator.class);
    Method findEligibleAdvisors = creator
            .getClass()
            .getSuperclass()
            .getSuperclass()
            .getDeclaredMethod("findEligibleAdvisors",Class.class, String.class);
    findEligibleAdvisors.setAccessible(true);
    List<Advisor> advisors = (List<Advisor>) findEligibleAdvisors.invoke(creator, Target2.class, "target2");
    advisors.forEach(System.out::println);
}
```
我们查看一下打印结果：
```aiignore
org.springframework.aop.interceptor.ExposeInvocationInterceptor.ADVISOR
org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [AspectJExpressionPointcut: () execution(* foo());]; advice [spring.aop.AdvisorAndAspect$Config$$Lambda/0x000002839b14b560@1cf6d1be]
InstantiationModelAwarePointcutAdvisor: expression [execution(* foo())]; advice method [public void spring.aop.AdvisorAndAspect$Aspect1.before()]; perClauseKind=SINGLETON
InstantiationModelAwarePointcutAdvisor: expression [execution(* foo())]; advice method [public void spring.aop.AdvisorAndAspect$Aspect1.after()]; perClauseKind=SINGLETON
```
这说明`creator`一共创造了4个切面，我们逐一解读：
* 第一个切面是Spring给将来所有代理都加入的切面，我们后面会详细讲解
* 剩下3个切面都是我们自己写的
  * `DefaultPointcutAdvisor`：我们自己写的低级切面
  * 另外两个是高级切面（aspect）转换后的低级切面（advisor）
    * `@Aspect`下有一个`@Before`和一个`@After`，两个低级切面

为什么`findEligibleAdvisors`方法认为这三个切面“有资格”呢？因为`[execution(* foo())]`表达式确实匹配到了方法，Spring能够为此创建切面。
我们现在来看看“没有资格”是什么情况：将`Target1`换成`Target2`。由于`Target2`中只有`bar`方法，所以表达式肯定是匹配不上的，因此Spring不能为`bar`方法创建切面：
```java
List<Advisor> advisors = (List<Advisor>) findEligibleAdvisors.invoke(creator, Target2.class, "target2");
advisors.forEach(System.out::println);
```
打印结果自然是什么都没有，因为没有任何切面产生。


### 2.3. `wrapIfNecessary()`
`wrapIfNecessary()`的内部调用了`findEligibleAdvisors()`，只要返回集合不为空，则表示需要创建代理对象。
```java
public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    /*...*/
    /* wrapIfNecessary */
    Method wrapIfNecessary = creator.getClass()
            .getSuperclass()
            .getSuperclass()
            .getSuperclass()
            .getDeclaredMethod("wrapIfNecessary", Object.class, String.class, Object.class);
    wrapIfNecessary.setAccessible(true);
    // 最后一个字符串参数不重要，我们可以随意指定
    Object o1 = wrapIfNecessary.invoke(creator, new Target1(), "target1", "target1");
    Object o2 = wrapIfNecessary.invoke(creator, new Target2(), "target2", "target2");
    System.out.println(o1.getClass());
    System.out.println(o2.getClass());
}
```
由于`Target1`中有匹配的方法而`Target2`中没有，所以我们预计`Target1`会被创建代理对象而`Target2`不会。打印结果如下：
```aiignore
class spring.aop.AdvisorAndAspect$Target1$$SpringCGLIB$$0
class spring.aop.AdvisorAndAspect$Target2
```
结果符合我们的预期。`Target1`被代理，生成的`o1`即为其代理对象。我们可以调用一下它的方法，看看其是否被增强：
```java
((Target1) o1).foo();
```
```aiignore
advice3 before... 
aspect1 before...
target1 foo
aspect1 after...
advice3 after...
```
一共被增强三次：高级切面中的前置和后置增强、低级切面中的环绕增强。

