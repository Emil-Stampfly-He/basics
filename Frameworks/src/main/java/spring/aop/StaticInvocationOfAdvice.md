# 静态通知调用

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

## 1. 统一转换为环绕通知
我们知道可以使用一些注解来表示通知：`@Before`、`@After`、`@Around`等等。这些注解对应的通知类为：
* `AspectJMethodBeforeAdvice`
* `AspectJAroundAdvice`
* `AspectJAfterReturningAdvice`
* `AspectJAfterThrowingAdvice`
* `AspectJAfterAdvice`

但最后它们都会统一被转换为`MethodInterceptor`。回顾之前的笔记，我们已经知道，`MethodInterceptor`是我们之前用来实现advice的类（回顾：一个通知本质上是一个拦截器），
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
接下来我们来看一看通知的转换。我们先准备如下代码：（对这一部分不熟的话可以参考[高级切面与低级切面（`@Aspect` vs Advisor）](https://github.com/Emil-Stampfly-He/basics/blob/333a7b5e1b82456b3aa83a57470c37f57f24dcf2/Frameworks/src/main/java/spring/aop/AdvisorAndAspect.md)）
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
在第2步中，我们创建了一个代理工厂，让它生成目标类的代理对象：（不熟悉的可以参考[Spring选择代理](https://github.com/Emil-Stampfly-He/basics/blob/af13cf62267557f2f7bcf6ad50b47a2b9ec9d4f1/Core/src/main/java/proxy/selective_proxy/SpringSelectiveProxy.md)）
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


## 4. 调用链执行
既然我们已经有了这么一些环绕通知，接下来就是创建并执行调用链了。回顾一下我们的调用链：
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
我们接着上面的`main`方法创建一个调用链。这里用到`MethodInvocation`下的`ReflectiveMethodInvocation`。另一个选项是`CglibIMethodInvocation`，但是这个类继承了`ReflectiveMethodInvocation`，所以是相似的。
`ReflectiveMethodInvocation`的构造器是`protected`，这里需要反射地调用构造方法：
```java
public static void main(String[] args) throws Throwable {
    // 1. 高级切面转换为低级切面
    /*...*/
    // 2. 通知统一转换为环绕通知MethodInterceptor
    /*...*/
    // 3. 统一转换成环绕通知
    /*...*/
    
    // 4. 创建并执行调用链（环绕通知 + 目标）
    Constructor<ReflectiveMethodInvocation> reflectiveMethodInvocationConstructor = ReflectiveMethodInvocation.class.getDeclaredConstructor(
            Object.class,
            Object.class,
            Method.class,
            Object[].class,
            Class.class,
            List.class
    );
    reflectiveMethodInvocationConstructor.setAccessible(true);
    ReflectiveMethodInvocation methodInvocation = reflectiveMethodInvocationConstructor.newInstance(
            null,
            new Aspect.Target(),
            Aspect.Target.class.getMethod("foo"),
            new Object[0],
            Aspect.Target.class,
            methodInterceptorList
    );

    // 5. 开始调用
    methodInvocation.proceed(); // 内部使用递归
}
```
创建完后就可以使用`proceed`方法进行调用了。我们运行：
```aiignore
around before
Exception in thread "main" java.lang.IllegalStateException: No MethodInvocation found: Check that an AOP invocation is in progress and that the ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor! In addition, ExposeInvocationInterceptor and ExposeInvocationInterceptor.currentInvocation() must be invoked from the same thread.
	at org.springframework.aop.interceptor.ExposeInvocationInterceptor.currentInvocation(ExposeInvocationInterceptor.java:74)
	at org.springframework.aop.aspectj.AbstractAspectJAdvice.getJoinPointMatch(AbstractAspectJAdvice.java:665)
	at org.springframework.aop.aspectj.AspectJMethodBeforeAdvice.before(AspectJMethodBeforeAdvice.java:44)
	at org.springframework.aop.framework.adapter.MethodBeforeAdviceInterceptor.invoke(MethodBeforeAdviceInterceptor.java:57)
	at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
	at org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint.proceed(MethodInvocationProceedingJoinPoint.java:89)
	at spring.aop.AspectToAdvisor$Aspect.around(AspectToAdvisor.java:49)
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
```
仅仅输出了一个前置增强就抛出了异常。异常的大意是：
* 找不到`MethodInvocation`这个调用链
* `ExposeInvocationInterceptor`必须在最前面

首先，很奇怪的一点是，我们已经创建了调用链，为什么说“找不到”？我们看看`proceed`方法里面到底发生了什么：`proceed`方法内部会调用到`AbstractAspectJAdvice`中的`getJoinPointMatch`方法。
而这个方法会去寻找`ExposeInvocationInterceptor.currentInvocation()`中是否有调用链。`ExposeInvocationInterceptor`这个类顾名思义是“暴露”调用链的一个通知（拦截器，advice的本质就是拦截器），它管理了一个`ThreadLocal`并把调用链放入这个`ThreadLocal`中。
既然我们没有创建一个`ExposeInvocationInterceptor`实例，也就是说`ThreadLocal`根本不存在，其他通知（`advice1`和`advice2`）无法找到调用链，所以就抛出了这个异常。

其次，还记得在[高级切面与低级切面（`@Aspect` vs Advisor）](https://github.com/Emil-Stampfly-He/basics/blob/215fd9f2e605ad34274d5f235586dd4fd78f6046/Frameworks/src/main/java/spring/aop/AdvisorAndAspect.md)第2.2节中的打印结果吗：
```aiignore
org.springframework.aop.interceptor.ExposeInvocationInterceptor.ADVISOR
```
我们明明只是定义了一个高级切面（内含两个低级切面）加一个低级切面，可是`AnnotationAwareAspectJAutoProxyCreator`却给我们格外创建了一个`ExposeInvocationInterceptor.ADVISOR`的切面。

结合这两点来看，这表明，要想让所有切面拿到同一个`ThreadLocal`中的调用链，我们必须默认地给所有切面外加一个最大的切面`ADVISOR`，这个切面功能是维护了一个`ThreadLocal`并把调用链放入其中，以便所有切面都能拿到同一个调用链——这也就是`ExposeInvocationInterceptor`所做的：
```aiignore
---------------------------------------------------------------------------
                                                                          |
    before1-----------------------------------------|                     |
                                                    |                     |
        before2-------------------------            |                     |
                                       |            |                     |
            target -------- 目标     advice2      advice1  (ExposeInvocationInterceptor.ADVISOR)
                                       |            |                   ADVICE
                                       |            |                     |
        after2--------------------------            |                     |
                                                    |                     |
    after1------------------------------------------|                     |
                                                                          |
---------------------------------------------------------------------------
```
到此，我们明白了，在给代理工厂加入切面`proxyFactory.addAdvisors(list)`之前，需要加入这个最大的切面：
```java
// 准备把MethodInvocation放入当前线程
// ExposeInvocationInterceptor是单例类
proxyFactory.addAdvice(ExposeInvocationInterceptor.INSTANCE);
proxyFactory.addAdvisors(list);
```
这样便能够正常运行了：
```aiignore
before1
before2
around before
Target foo
around after
afterReturning
```
确实是层状调用的模式。当然，我们这里忽略了通知调用的顺序。如果要指定顺序，需要将每个通知都放入一个高级切面类并使用`@Order`进行顺序的指定，这里不再展开。

## 5. `proceed`责任链模式
最后我们来看看启动调用链的方法`proceed`，它的内部使用了递归。以下是分步骤解析：（由于还没有谈到动态匹配器，所以相关的逻辑我们先跳过）
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
这里看上去没有使用到递归，但是我们仔细思考一下，通知是这样被定义的：
```java
public MethodInterceptor advice1() {
    return invocation -> {
        System.out.println("advice1 before... ");
        Object result = invocation.proceed();
        System.out.println("advice1 after...");
        return result;
    };
}
```
所以：（下面忽略了最外层的`ExposeInvocationInterceptor.ADVISOR`）
* 拦截器（通知）调用`invoke(this)`，方法会返回到`advice1`中，而`advice1`会继续调用`proceed`
* 如果`advice1`中还有`advice2`，那么`proceed`方法继续调用`invoke(this)`, 方法返回到`advice2`中再调用`proceed`
* 如果`advice2`后就是目标方法了，那就调用目标方法，再一层层返回：`advice2` → `advice1`

为什么说这是责任链模式：

  | 责任链模式要素               | 在 Spring AOP 中的实现                                                     |
  | --------------------- | --------------------------------------------------------------------------- |
  | 抽象处理者 `Handler`         | `org.aopalliance.intercept.MethodInterceptor`                               |
  | 具体处理者 `ConcreteHandler` | 不同类型的 Advice（`BeforeAdviceInterceptor`、`AfterReturningAdviceInterceptor`、环绕增强等） |
  | 请求 `Request`            | `ReflectiveMethodInvocation` 对象本身（包含 `method`、`args`、`target` 等）        |
  | 链上节点 `Chain`            | `interceptorsAndDynamicMethodMatchers` 列表                                   |
  | 传递逻辑                  | `proceed()` 方法                                                              |


到此，静态通知调用就讲解完毕了。