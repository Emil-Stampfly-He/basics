# 静态调用通知

## 1. 统一转换为环绕通知
我们知道可以使用一些注解来表示通知：`@Before`、`@After`、`@Around`等等。这些注解对应的通知类为：
* `AspectJMethodBeforeAdvice`
* `AspectJAroundAdvice`
* `AspectJAfterReturningAdvice`
* `AspectJAfterThrowingAdvice`
* `AspectJAfterAdvice`

但最后它们都会统一被转换为`MethodInterceptor`。回顾之前的笔记，我们已经知道，`MethodInterceptor`是我们之前用来实现advice的类（通知类），
在这个类中我们可以实现任何增强方法的逻辑。这就是为什么说最后都会被转换为“环绕”通知的原因（并不是说都会被转换为`@Around`，只是说会实现相同的功能）。

为什么我们会需要将所有通知都转换为环绕通知呢？我们来看看有多个advisor的情况：当前置增强开始时，我们肯定希望在目标方法被调用之前执行的全都是前置增强，而不是穿插着后置增强。
那么调用就会像下图所示：
```aiignore
before1-----------------------------------------|
                                                |
    before2-------------------------            |
                                   |            |
        target -------- 目标     advice2      advice1
                                   |            |
    after2--------------------------            |
                                                |
after1------------------------------------------|
```
先调用`before1`，再进入`advice2`调用`before2`。前置增强结束，调用目标方法；再一层层出来，先调用`advice2`的`after2`，最后调用`after1`。
我们要想实现这样的调用链，自然是环绕通知是最合适的，因为只有环绕通知能够形成这样的层状关系。

`MethodInterceptor`本身是一个接口，如果实现了该接口，就说明该通知已经是一种环绕通知了，不会再被转换。以下几个通知是已经实现了`MethodInterceptor`接口的：
* `AspectJAroundAdvice` → `@Around`
* * `AspectJAfterThrowingAdvice` → `@AfterThrowing`
* `AspectJAfterAdvice` → `@After`

另外两个没有实现，因此需要被转换。

## 2. 通知的转换：`getInterceptorsAndDynamicInterceptionAdvice`
接下来我们来看一看通知的转换。我们先准备如下代码：（对这一部分不熟的话可以参考[高级切面与低级切面（`@Aspect` vs Advisor）](https://github.com/Emil-Stampfly-He/basics/blob/333a7b5e1b82456b3aa83a57470c37f57f24dcf2/Frameworks/src/main/java/spring/aop/AdvisorAndAspect.md)
> 注意：我们省去了对advisor排序的步骤。在实际中运用中我们是可以指定切面顺序的。这里为了简便所以省去。

```java
public class AspectToAdvisor {
    static class Aspect {
        @Before("execution(* foo())")
        public void before1() {System.out.println("before1");}
        @Before("execution(* foo())")
        public void before2() {System.out.println("before2");}
        /*public void after() {System.out.println("after");}*/
        @AfterReturning("execution(* foo())")
        public void afterReturning() {System.out.println("afterReturning");}
        /*public void afterThrowing() {System.out.println("afterThrowing");}*/
        @Around("execution(* foo())")
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            System.out.println("around before");
            Object result = pjp.proceed();
            System.out.println("around after");
            return result;
        }

        static class Target { public void foo() {System.out.println("Target foo");}}

        // 1. 高级切面转换为低级切面
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
                    // 通知类
                    AspectJMethodBeforeAdvice beforeAdvice = new AspectJMethodBeforeAdvice(method, pointcut, factory);
                    // advisor
                    Advisor advisor = new DefaultPointcutAdvisor(pointcut,  beforeAdvice);
                    list.add(advisor);
                } else if (method.isAnnotationPresent(Around.class)) {
                    // 解析切点
                    Around around = method.getAnnotation(Around.class);
                    assert around != null;
                    String pointcutExpression = around.value();
                    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                    pointcut.setExpression(pointcutExpression);
                    // 通知类
                    AspectJAroundAdvice beforeAdvice = new AspectJAroundAdvice(method, pointcut, factory);
                    // advisor
                    Advisor advisor = new DefaultPointcutAdvisor(pointcut, beforeAdvice);
                    list.add(advisor);
                } else if (method.isAnnotationPresent(AfterReturning.class)) {
                    // 解析切点
                    AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                    assert afterReturning != null;
                    String pointcutExpression = afterReturning.value();
                    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                    pointcut.setExpression(pointcutExpression);
                    // 通知类
                    AspectJAfterReturningAdvice beforeAdvice = new AspectJAfterReturningAdvice(method, pointcut, factory);
                    // advisor
                    Advisor advisor = new DefaultPointcutAdvisor(pointcut, beforeAdvice);
                    list.add(advisor);
                }
            }

            list.forEach(System.out::println);

            // 2. 通知统一转换为环绕通知MethodInterceptor
            ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setTarget(new Aspect.Target()); // 设置目标类
            proxyFactory.setInterfaces(Aspect.Target.class.getInterfaces()); // 设置目标类接口
            proxyFactory.addAdvisors(list); // 加入advisors（低级切面）

            // 3. 统一转换成环绕通知
            List<Object> methodInterceptorList = proxyFactory.getInterceptorsAndDynamicInterceptionAdvice(Aspect.Target.class.getMethod("foo"), Aspect.Target.class);
            methodInterceptorList.forEach(System.out::println);
        }
    }
}
```
在第2步中，我们创建了一个代理工厂，让它生成目标类的代理对象：（不熟悉的可以参考[Spring选择代理](https://github.com/Emil-Stampfly-He/basics/blob/af13cf62267557f2f7bcf6ad50b47a2b9ec9d4f1/Core/src/main/java/proxy/selective_proxy/SpringSelectiveProxy.md)
```java
// 2. 通知统一转换为环绕通知MethodInterceptor
ProxyFactory proxyFactory = new ProxyFactory();
proxyFactory.setTarget(new Aspect.Target()); // 设置目标类
proxyFactory.setInterfaces(Aspect.Target.class.getInterfaces()); // 设置目标类接口
proxyFactory.addAdvisors(list); // 加入advisors（低级切面）
```
当然，我们的`Target`类没有实现任何接口，所以“设置接口”这一步是可以省略的。

接下来在第3步中，有一个至关重要的方法`getInterceptorsAndDynamicInterceptionAdvice`。这个方法的作用就是将非环绕通知转换为环绕通知。
我们可以查看一下打印结果来看看这个方法的输出：

```aiignore
org.springframework.aop.framework.adapter.AfterReturningAdviceInterceptor@5891e32e
org.springframework.aop.aspectj.AspectJAroundAdvice: advice method [public java.lang.Object spring.aop.AspectToAdvisor$Aspect.around(org.aspectj.lang.ProceedingJoinPoint) throws java.lang.Throwable]; aspect name ''
org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@cb0ed20
org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor@8e24743
```
由于`@Before`和`@AfterReturning`都不是环绕通知，所以被分别转换为了`MethodBeforeAdviceInterceptor`和`AfterReturningAdviceInterceptor`。这两个类都已经实现了`MethodInterceptor`接口，说明被转换为了环绕通知。
而`@Around`已经是环绕通知了，所以保持了原来的`AspectJAroundAdvice`类，没有被转换。

## 3. 设计模式：适配器模式
适配器模式简单来讲，就是把一套接口转换为另一套接口，以便适合某种场景的通用。该设计模式在Spring中被大量使用。实际上，统一转换为`MethodInterceptor`就是对这一模式的体现。而做转换工作的这个类被称作“适配器”：
* `MethodBeforeAdviceAdapter`将`@Before`的`AspectJMethodBeforeAdvice`适配为`MethodBeforeAdviceInterceptor`
* `AfterReturningAdviceAdapter`将`@AferReturning`的`AspectJAfterReturningAdvice`适配为`AfterReturningAdviceInterceptor`

我们看看适配器`MethodBeforeAdviceAdapter`的源码：
```java
class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {
	@Override
	public boolean supportsAdvice(Advice advice) {
		return (advice instanceof MethodBeforeAdvice);
	}

	@Override
	public MethodInterceptor getInterceptor(Advisor advisor) {
		MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
		return new MethodBeforeAdviceInterceptor(advice);
	}
}
```
很简单，`supportsAdvice`方法用来判断这个适配器支持哪种通知类，`getInterceptor`用来进行转换。

