# 动态通知调用

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)
> 
> 注意：学习本章内容之前，建议先熟悉[静态通知调用](https://github.com/Emil-Stampfly-He/basics/blob/09f5c2816dbb8cca14f412d712cc451e7d85cd87/Frameworks/src/main/java/spring/aop/StaticInvocationOfAdvice.md)的内容

在[静态通知调用](https://github.com/Emil-Stampfly-He/basics/blob/09f5c2816dbb8cca14f412d712cc451e7d85cd87/Frameworks/src/main/java/spring/aop/StaticInvocationOfAdvice.md)一节中，我们还有一个悬而未决的问题。
我们谈到了调用链`MethodInvocation`中的`proceed`方法：
```java
// ReflectiveMethodInvocation.java
@Override
@Nullable
public Object proceed() throws Throwable {
// 1. 如果拦截器（通知）已经递归地调用完毕，则直接调用目标方法（我们讲的是target，这里用joinpoint表示）
// 结束递归
if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
return invokeJoinpoint();
}

    // 2. 否则，取下一个拦截器或动态匹配器（我们先不管什么是动态匹配器）
    Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    // 3. 如果是动态匹配器，运行下面的逻辑（动态匹配器的部分我们跳过）
    if (/*...*/) {
        /*...*/
    }
    // 4. 如果是拦截器（通知），则调用invoke方法
    else {
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}
```
我们当时省略了第2、3步逻辑的讲解，现在是时候具体看看什么是“动态匹配器”了。

## 1. 准备工作
我们先准备好以下代码：
* 一个含有两个前置通知的`@Aspect`高级切面
* 一个`Target`目标类
* 将两个bean注入

`main`方法的逻辑是：
* 获取并初始化一个bean容器
* 获取之前注入的两个bean
* 通过`AnnotationAwareAspectJAutoProxyCreator#findEligibleAdvisors`获得一个切面列表 
* 创建代理工厂以及获取代理对象
* 通过代理工厂获取通知类型
```java
public class DynamicInvocationOfAdvice {
    @Aspect
    static class MyAspect {
        @Before("execution(* foo(..))") // 静态通知调用，不带参数绑定，执行时不需要切点
        public void before1() {
            System.out.println("before 1");
        }
        // 动态通知调用，需要参数绑定，性能更低，执行时仍需要切点
        // 静态部分在代理创建时就已经筛掉不可能的目标方法
        // 动态部分时会需要通过MethodMather.matches方法再跑一次，只有返回true才真正执行before2
        @Before("execution(* foo(..)) && args(x)")
        public void before2(int x) {
            System.out.printf("before 2 %d\n", x);
        }
    }

    static class Target {
        public void foo(int x) {
            System.out.printf("target foo(%d)%n", x);
        }
    }

    @Configuration
    static class MyConfig {
        @Bean
        AnnotationAwareAspectJAutoProxyCreator proxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }

        @Bean
        public MyAspect myAspect() {return new MyAspect();}
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Throwable {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(ConfigurationClassPostProcessor.class);
        context.registerBean(MyConfig.class);
        context.refresh();

        AnnotationAwareAspectJAutoProxyCreator creator = context.getBean(AnnotationAwareAspectJAutoProxyCreator.class);
        Method findEligibleAdvisors = creator
                .getClass()
                .getSuperclass()
                .getSuperclass()
                .getDeclaredMethod(
                "findEligibleAdvisors", Class.class, String.class);
        findEligibleAdvisors.setAccessible(true);
        List<Advisor> list = ((List<Advisor>) findEligibleAdvisors.invoke(creator, Target.class, "target"));

        Target target = new Target();
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisors(list);
        Target proxy = ((Target) proxyFactory.getProxy());

        List<Object> interceptorsList = proxyFactory.getInterceptorsAndDynamicInterceptionAdvice(Target.class.getMethod("foo", int.class), Target.class);
        interceptorsList.forEach(System.out::println);
    }
}
```
注意，`@Aspect`高级切面中的第二个前置方法是动态增强的：`@Before("execution(* foo(..)) && args(x)")`，这意味着切点不仅会根据方法的名字进行匹配，还会在运行时检查参数列表，根据参数的数量去匹配那些只有一个参数的方法。

我们可以打印一下结果，先看看通知（回顾：通知的本质是拦截器）都有哪些种类：
```aiignore
org.springframework.aop.interceptor.ExposeInvocationInterceptor@5340477f
org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@47caedad
InterceptorAndDynamicMethodMatcher[interceptor=org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@28cda624, matcher=AspectJExpressionPointcut: (int x) execution(* foo(..)) && args(x)]
```
前两个我们并不陌生：一个是外围的最大通知`ExposeInvocationInterceptor`，还有一个是第一个前置通知，只不过被转换为了环绕通知`MethodBeforeAdviceInterceptor`。这些都是我们已经见到过的静态通知。
但是第三个`InterceptorAndDynamicMethodMatcher`，我们点进源码中会发现它并没有实现`MethodInterceptor`接口，说明并不是被转换成了环绕通知。这个新的类型其实就对应着我们现在要深入的动态通知。

## 2. `InterceptorAndDynamicMethodMatcher`
虽然`InterceptorAndDynamicMethodMatcher`没有实现`MethodInterceptor`，但是`MethodInterceptor`是它的一个成员变量。同时它还有另一个成员变量`MethodMatcher`：
```java
record InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher matcher) {}
```
> `record`我们就简单地当成是`class`，这里不会深入讲解这个关键字和特性。

`MethodMatcher`乍一看好像没见过，实际上我们之前使用过的切点类`AspectJExpressionPointcut`，它所继承的接口`IntroductionAwareMethodMatcher`是`MethodMatcher`的一个子接口。
所以这个`MethodMatcher`就是切点。也就是说，这个“拦截器与动态方法匹配器”是由一个环绕通知和一个切点组成的。这个组成很好理解，因为`@Before("execution(* foo(..)) && args(x)")`被拆成两部分后，前一部分需要被转换成一个环绕通知，而后一部分需要在运行时去切入方法。

我们针对`InterceptorAndDynamicMethodMatcher`打印一下里面的细节：
```java
public static void main(String[] args) throws Throwable {
    // 以上均不变，将下面的forEach中的方法从打印变成showDetails
    interceptorsList.forEach(DynamicInvocationOfAdvice::showDetails);
}

public static void showDetails(Object o) {
    try {
        Class<?> clazz = Class.forName("org.springframework.aop.framework.InterceptorAndDynamicMethodMatcher");
        if (clazz.isInstance(o)) {
            Field matcher = clazz.getDeclaredField("matcher");
            Field interceptor = clazz.getDeclaredField("interceptor");
            matcher.setAccessible(true);
            interceptor.setAccessible(true);

            System.out.println("环绕通知和切点：" + o);
            System.out.println("\t切点为：" + matcher.get(o));
            System.out.println("\t通知为" + interceptor.get(o));
        } else {
            System.out.println("普通环绕通知：" + o);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```
```aiignore
普通环绕通知：org.springframework.aop.interceptor.ExposeInvocationInterceptor@7857fe2
普通环绕通知：org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@6f15d60e
环绕通知和切点：InterceptorAndDynamicMethodMatcher[interceptor=org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@446a1e84, matcher=AspectJExpressionPointcut: (int x) execution(* foo(..)) && args(x)]
	切点为：AspectJExpressionPointcut: (int x) execution(* foo(..)) && args(x)
	通知为org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@446a1e84
```
可以看到，确实是由一个表达式切点和一个前置通知转成的环绕通知组成。

## 3. 完整的`proceed`
现在我们可以来看看完整的`proceed`方法到底是怎么回事了。
```java
@Override
@Nullable
public Object proceed() throws Throwable {
    // We start with an index of -1 and increment early.
    if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
        return invokeJoinpoint();
    }

    Object interceptorOrInterceptionAdvice =
            this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher dm) {
        // Evaluate dynamic method matcher here: static part will already have
        // been evaluated and found to match.
        Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
        if (dm.matcher().matches(this.method, targetClass, this.arguments)) {
            return dm.interceptor().invoke(this);
        }
        else {
            // Dynamic matching failed.
            // Skip this interceptor and invoke the next in the chain.
            return proceed();
        }
    }
    else {
        // It's an interceptor, so we just invoke it: The pointcut will have
        // been evaluated statically before this object was constructed.
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}
```
当我们执行`proxy.foo()`的时候，`ReflectiveMethodInvocation#proceed`就会被调用。逻辑如下：
1. 先判断是不是递归终点（即目标方法），如果是则退出递归
2. 判断通知是动态通知还是静态通知
   1. 如果是动态通知且匹配上了方法，调用动态拦截器的`invoke`
   2. 如果是动态通知但是没匹配上，则跳过
3. 如果通知是静态通知，那么直接调用静态通知拦截器的`invoke`

可以看出，其实逻辑跟静态通知调用是非常相似的，只不过是多了一个匹配器而已。
