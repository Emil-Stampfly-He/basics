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
创建bean实例 ->（*）依赖注入 -> 初始化（*）-> ...
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
* 第一个切面是Spring给将来所有代理都加入的切面，后续在[静态调用通知](https://github.com/Emil-Stampfly-He/basics/blob/215fd9f2e605ad34274d5f235586dd4fd78f6046/Frameworks/src/main/java/spring/aop/StaticInvocationOfAdvice.md)的第4节中会详细讲解
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


## 3. 高级切面转换为低级切面
为了研究清楚`findEligible`的实现逻辑，我们现在来手动实现一下它所能实现的功能。
我们首先准备一个高级切面，里面有两个前置通知，针对的是`Target`中的`foo`方法：
```java
public class AspectToAdvisor {
    static class Aspect {
        @Before("execution(* foo())") public void before1() {System.out.println("before1");}
        @Before("execution(* foo())") public void before2() {System.out.println("before2");}
//        public void after() {System.out.println("after");}
//        public void afterReturning() {System.out.println("afterReturning");}
//        public void afterThrowing() {System.out.println("afterThrowing");}
//        public Object around(ProceedingJoinPoint pjp) throws Throwable {
//            System.out.println("around before");
//            Object result = pjp.proceed();
//            System.out.println("around after");
//            return result;
//        }
        // 目标类
        static class Target { public void foo() {System.out.println("Target foo");}}
    }
}
```
当然，Spring AOP中提供的通知注解不止`@Before`一种，还有`@After`、`@Around`、`@After`等。这里我们暂时先用`@Before`举例。

为了将高级切面拆解为低级切面，我们整体的思路是这样的：
1. 反射地获取切面类中所有带有`@Before`注解的方法
2. 通过`@Before`中的值获取切点
3. 创建通知（advice）：因为是前置通知，所以使用`AspectJMethodBeforeAdvice`。
   * 如果是别的通知类型，可以使用其他名称相似的类
     * `AspectJAroundAdvice`
     * `AspectJAfterReturningAdvice`
     * `AspectJAfterThrowingAdvice`
     * `AspectJAfterAdvice`
4. 将切点与通知组合成低级切面（advisor）
5. 最后将这些低级切面放到一个集合中
```java
public static void main(String[] args) {
    AspectInstanceFactory factory = new SingletonAspectInstanceFactory(new Aspect());
    List<Advisor> list = new ArrayList<>();
    for (Method method: Aspect.class.getDeclaredMethods()) {
        if (method.isAnnotationPresent(Before.class)) {
            // 解析切点
            Before before = method.getAnnotation(Before.class);
            assert before != null;
            String pointcutExpression = before.value();
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression(pointcutExpression);
            // 通知类，需要传入一个切面实例工厂，作用是创建一个切面实例对象（光有方法method没用，将来要调用的话必须有一个实例来调用）
            AspectJMethodBeforeAdvice beforeAdvice = new AspectJMethodBeforeAdvice(method, pointcut, factory);
            // advisor：切点 + 通知
            Advisor advisor = new DefaultPointcutAdvisor(pointcut, beforeAdvice);
            list.add(advisor);
        }
    }
    list.forEach(System.out::println);
}
```
输出结果如下。我们可以看到，一个`Aspect`高级切面类确实被解析成了两个低级的切面：
```aiignore
org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [AspectJExpressionPointcut: () execution(* foo())]; advice [org.springframework.aop.aspectj.AspectJMethodBeforeAdvice: advice method [public void spring.aop.AspectToAdvisor$Aspect.before1()]; aspect name '']
org.springframework.aop.support.DefaultPointcutAdvisor: pointcut [AspectJExpressionPointcut: () execution(* foo())]; advice [org.springframework.aop.aspectj.AspectJMethodBeforeAdvice: advice method [public void spring.aop.AspectToAdvisor$Aspect.before2()]; aspect name '']
```


## 4. 代理对象创建时机

回顾一下上一节的内容：
>`AnnotationAwareAspectJAutoProxyCreator`自己本身作为一个bean，会在两个时间点发挥作用：依赖注入前与初始化后（*为发挥作用时间点）：
>```aiignore
>Bean的生命周期：
>创建bean实例 ->（*）依赖注入 -> 初始化（*）-> ...
>```
必须注意的是，两个发挥作用时间点是二选一的，不会重复创建代理对象。那么，什么时候代理发生在依赖注入之前，什么时候发生在初始化之后呢？

### 4.1. Bean之间是单向依赖关系
我们创建两个Bean：`Bean1`单向地依赖`Bean2`。中间注入的一些Bean是一些为切面生成代理以及依赖注入的必要配置。
```java
public class ProxyConstructTiming {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(ConfigurationClassPostProcessor.class);
        context.registerBean(Config.class);
        context.refresh();
        context.close();
        // 创建 →（*）依赖注入 → 初始化（*）
    }

    @Configuration
    static class Config {
        @Bean // 解析@Aspect，自动生成代理
        public AnnotationAwareAspectJAutoProxyCreator annotationAwareAspectJAutoProxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }
        @Bean // 解析@Autowired
        public AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor() {
            return new  AutowiredAnnotationBeanPostProcessor();
        }
        @Bean // 解析@PostConstruct
        public CommonAnnotationBeanPostProcessor commonAnnotationBeanPostProcessor() {
            return new  CommonAnnotationBeanPostProcessor();
        }
        @Bean
        public Advisor advisor(MethodInterceptor advice) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* foo())");
            return new DefaultPointcutAdvisor(pointcut, advice);
        }
        @Bean
        public MethodInterceptor advice() {
            return invocation -> {
                System.out.println("before...");
                return invocation.proceed();
            };
        }
        @Bean
        public Bean1 bean1() { return new Bean1(); }
        @Bean
        public Bean2 bean2() { return new Bean2(); }
    }

    static class Bean1 {
        public void foo() {}
        public Bean1() {System.out.println("Bean1()");}
        @PostConstruct public void init() {System.out.println("Bean1 init()");}
    }

    static class Bean2 {
        public Bean2() {System.out.println("Bean2()");}
        @Autowired public void setBean1(Bean1 bean1) {
            System.out.println("Bean2 setBean1(bean1) class is: " + bean1.getClass().getName()");
        }
        @PostConstruct public void init() {System.out.println("Bean2 init()");}
    }
}
```
我们打印，可以看到“创建CGLIB代理对象”的日志在`Bean1 init()`后才出现，这说明代理对象是在Bean初始化之后创建的。

### 4.2. Bean之间是循环依赖关系
我们现在调整一下依赖关系：让`Bean1`和`Bean2`互相依赖彼此：
```java
static class Bean1 {
    public void foo() {}
    public Bean1() {System.out.println("Bean1()");}
    @Autowired public void setBean2(Bean2 bean2) {
        System.out.println("Bean1 setBean2(bean2) class is: " + bean2.getClass().getName());
    }
    @PostConstruct public void init() {System.out.println("Bean1 init()");}
}

static class Bean2 {
    public Bean2() {System.out.println("Bean2()");}
    @Autowired public void setBean1(Bean1 bean1) {
        System.out.println("Bean2 setBean1(bean1) class is: " + bean1.getClass().getName());
    }
    @PostConstruct public void init() {System.out.println("Bean2 init()");}
}
```
打印之后，可以发现`Bean1`被创建了一个CGLIB代理对象，而`Bean2`没有。这一点很好理解，因为没有任何切点匹配上了`Bean2`里面的任何方法。

我们还能看到“创建CGLIB代理对象”的日志在`Bean1 init()`前就出现了。对此，一个直观的解释是：由于`Bean1`中的功能被增强，所以`Bean2`需要注入一个拥有增强方法的`Bean1`，因此`Bean1`必须提前被代理增强。
然后`Bean2`的注入完成，就可以接着继续完成初始化了。接着，`Bean1`开始接受`Bean2`的注入，并进行初始化。

**总而言之：如果发生循环依赖，则代理会被提前，因为依赖方需要被增强的被依赖方。如果不存在循环依赖关系，则代理发生在初始化后。**
> 补充：源码调用链 
> 
> 在`AbstractAutowireCapableBeanFactory`中有一个`doCreateBean()`方法和一个`allowCircularReference`字段。这个字段顾名思义是用来判断循环依赖是否存在的。
> 如果当前的bean是单例bean且`allowCircularReference`字段为`true`，那么`doCreateBean()`方法就会调用`getEarlyBeanReference()`方法，也就是提前进行代理（也就是提前调用`wrapIfNecessary()`）。