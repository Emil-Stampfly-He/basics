# `@Aspect` vs Advisor

## 1. 准备工作
```java
public class AdvisorAndAspect {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("aspect1", Aspect1.class);
        context.registerBean("config", Config.class);
        context.registerBean(ConfigurationClassPostProcessor.class);
        context.refresh();
        
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }
    }

    static class Target1 { public void foo() {System.out.println("target1 foo");}}
    static class Target2 { public void bar() {System.out.println("target2 bar");}}

    @Aspect
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

    @Configuration
    static class Config { // 低级切面
        @Bean // advice需要作为bean传入
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
输出结果如下：
```aiignore
aspect1
config
org.springframework.context.annotation.ConfigurationClassPostProcessor
advisor3
advice3
```
可以看到，容器将低级切面分别解析出了advisor和通知。

## 2. `AnnotationAwareAspectJAutoProxyCreator`
两个非常重要的方法：
1. `findEligibleAdvisors()` \
该方法继承自`AbstractAdvisorAutoProxyCreator`，用于寻找“有资格”的低级切面（advisors）并将它们加入到一个集合中。高级切面会先被解析成低级切面（advisor）之后加入其中。
2. `wrapIfNecessary()` \
该方法继承自`AbstractAutoProxyCreator`。如果有必要，则为这个bean创建代理（也就是wrap的意思）。那么怎样才是“有必要”呢？
   * 目标类确实有匹配的切面
   * 如果目标类根本没有匹配的切面，则不创建代理

由于这两个方法都是`protected`，所以下面我们使用反射调用：
```java
AnnotationAwareAspectJAutoProxyCreator creator = context.getBean(AnnotationAwareAspectJAutoProxyCreator.class);
Method findEligibleAdvisors = creator
        .getClass()
        .getSuperclass()
        .getSuperclass()
        .getDeclaredMethod("findEligibleAdvisors",Class.class, String.class);
findEligibleAdvisors.setAccessible(true);
List<Advisor> advisors = (List<Advisor>) findEligibleAdvisors.invoke(creator, Target1.class, "target1");
advisors.forEach(System.out::println);
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



