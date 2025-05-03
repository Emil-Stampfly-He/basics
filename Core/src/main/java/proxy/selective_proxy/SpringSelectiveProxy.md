# Spring选择代理

更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)
>**本笔记基于黑马程序员 Spring高级源码解读**

## 0. 前置知识：切面、通知、切点
切面（aspect）由通知（advice）和切点（pointcut）组成的一个或多个顾问（advisor，有人将advisor也称为切面，用来指代更基本更底层的切面）共同构成。
切点负责匹配程序中的连接点，通知则定义在匹配到的连接点上执行的操作。顾问则将切点与通知捆绑为一个最小的切面单元，而多个顾问聚合后构成了一个完整的切面。

构成图如下所示：
```aiignore
两个切面概念: aspect & advisor
    aspect =
        advice1（通知）+ pointcut1（切点）= advisor1
        advice2 + pointcut2 = advisor2
        advice3 + pointcut3 = advisor3
        ...
        
advisor：更细粒度的切面，包含一个通知和一个切点
aspect：由多个advisors组成
```
> 注意：本文中有时也称advisor为切面，但是都明确注明了“切面”是指代advisor还是aspect。

## 1. 手动AOP
作为准备，我们先创建一个接口和一个目标类：
```java
public class SpringSelectiveProxy {
    interface I1 { void foo();void bar();}
    static class Target1 implements I1 {
        public void foo() {System.out.println("target1 foo");}
        public void bar() {System.out.println("target1 bar");}
    }
}
```

为了实现一个advisor，我们需要准备一个切点`pointcut`和一个通知`advice`：
* `pointcut` \
切点的类有很多中，最常用的是：`AspectJExpressionPointCut`和`AnnotationMatchingPointcut`，分别对应AOP注解的`execution`和`@annatation`两种方式。
> ```java
> // Spring中我们这样装配AOP配置
> @Aspect
> static class MyAspect {
>   @Before("execution (* foo())") // AspectJExpressionPointcut
>   public void before() {System.out.println("before myAspect");}
>   @After("@annotation(MyAnnotation.class)") // AnnotationMatchingPointcut
>   public void after() {System.out.println("after myAspect");}
> }
>```
* `advice` \
一个通知`advice`本质上就是一个拦截器，使用`MethodInterceptor`进行方法的拦截与增强。

有了`advisor`，我们就让代理工厂绑定切面，并创建出代理对象：
```java
public class SpringSelectiveProxy {
    public static void main(String[] params) {
        // 1. 备好切点: pointcut
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution (* foo(..))"); // 只增强foo方法
        // 2. 备好通知: advice
        MethodInterceptor advice = invocation -> {
            System.out.println("before myAspect");
            Object result = invocation.proceed();
            System.out.println("after myAspect");
            return result;
        };
        // 3. 备好advisor：advice + pointcut
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

        // 4. 创建代理
        ProxyFactory factory = new ProxyFactory();
        Target1 target1 = new Target1();
        factory.setTarget(target1); // 设置目标类
        factory.addAdvisor(advisor); // 绑定切面

        I1 proxy = ((I1) factory.getProxy()); // 获得代理对象
        System.out.println(proxy.getClass().getName());

        proxy.foo(); // 被增强
        proxy.bar(); // 未被增强
    }

    interface I1 { void foo();void bar();}

    static class Target1 implements I1 {
        public void foo() {System.out.println("target1 foo");}
        public void bar() {System.out.println("target1 bar");}
    }
}
```
输出结果：
```aiignore
proxy.selective_proxy.SpringSelectiveProxy$Target1$$SpringCGLIB$$0
before myAspect
target1 foo
after myAspect
target1 bar
```
`foo`方法被增强，`bar`方法未被增强，这符合我们的预期。我们成功实现了一个切面并让代理对象针对切面进行了增强。

## 2. JDK动态代理与CGLIB动态代理的条件
我们再仔细看看上一节的输出结果：`SpringSelectiveProxy$Target1$$SpringCGLIB$$0`。这说明代理工厂使用了CGLIB。为什么没用JDK呢？

首先我们需要明确一下代理工厂选择JDK / CGLIB的条件：
* `proxyTargetClass = false` && 目标实现接口：JDK
* `proxyTargetClass = false` && 目标未实现接口：CGLIB
* `proxyTargetClass = true`：CGLIB

其中，`proxyTargetClass`是`ProxyConfig`的一个字段，用于表明是否直接代理目标对象还是代理目标对象的接口。

但在我们这个例子中，既然`Target1`继承了`I1`，为什么Spring还会选择CGLIB进行代理呢？
这是因为工厂并不能直接知道目标类是否实现了接口。所以我们需要显式地为工厂设置目标类的接口：
```java
factory.setInterfaces(target1.getClass().getInterfaces());
```
这样，工厂就能够知道目标类是实现了接口的，然后使用JDK代理。加入上面这一行代码后再打印一次：
```aiignore
proxy.selective_proxy.$Proxy2
```
工厂选择了JDK动态代理，符合我们的预期。

我们再来看看目标类没有实现接口的情况。由于JDK动态代理必须基于接口，所以在没有接口的情况下，工厂肯定会选择CGLIB代理：

```java
public class SpringSelectiveProxy {
    public static void main(String[] params) {
        /*...*/
        ProxyFactory factory = new ProxyFactory();
        Target2 target2 = new Target2();
        factory.setTarget(target2); // 设置目标类
        factory.addAdvisor(advisor); // 绑定切面

        Target2 proxy = (Target2) factory.getProxy(); // 获得代理对象
        System.out.println(proxy.getClass().getName()); // 使用CGLIB代理

        proxy.foo(); // 被增强
        proxy.bar(); // 未被增强
    }

    interface I1 { void foo(); void bar();}
    static class Target1 implements I1 {/*...*/}

    static class Target2 {
        public void foo() {System.out.println("target2 foo");}
        public void bar() {System.out.println("target2 bar");}
    }
}
```
我们可以输出打印一下，结果一定是符合我们的预期的。

## 3. `factory.getProxy()`的源码调用链

1. `main`方法中调用`factory.getProxy()`
2. `getProxy()`调用`ProxyFactory`中的`createAopProxy()`
   1. `createProxy()`首先激活`AdvisedSupportListener`监听切面消息，再调用`ProxyCreatorSupport`中的`getAopProxyFactory()`
   2. `getAopProxyFactory()`会调用`DefaultAopProxyFactory`的`createAopProxy(AdvisedSupport config)`对AOP工厂种类进行指定
      * `proxyTargetClass = false` && 目标实现接口：`JDKDynamicAopFactory`
      * `proxyTargetClass = false` && 目标未实现接口 `ObjenesisCglibAopFactory`
      * `proxyTargetClass = true`：`ObjenesisCglibAopFactory`
3. 指定的AOP工厂返回给`createAopProxy()`后，调用该AOP工厂下的`getProxy()`方法，这个方法会调用`Proxy.newProxyInstance()`生成一个代理对象
   * `JDKDynamicAopFactory`会生成一个JDK代理对象
   * `ObjenesisCglibAopFactory`会生成一个CGLIB代理对象
4. 最终通过`factory.getProxy()`拿到动态代理对象