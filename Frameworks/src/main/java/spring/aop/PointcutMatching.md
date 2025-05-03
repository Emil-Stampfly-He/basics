# 切点匹配

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

## 1. 两种切点匹配模式
我们经常这样装配我们的AOP：
```java
@Component
@Aspect
public class MyAspect {
    @Before("execution(* bar())")
    public void before() {/*...*/}
    @After(@annotation(org.springframework.transaction.annotation.Transactional))
    public void after() {/*...*/}
}
```
一种是根据`execution`表达式进行方法的匹配，另一种是根据`@annotation`寻找方法上是否有特定的注解进行匹配。
我们可以手动装配切点：
```java
public class PointcutMatching {

    public static void main(String[] args) throws Exception {
        AspectJExpressionPointcut pointcut1 = new AspectJExpressionPointcut();
        // 根据方法名字匹配
        pointcut1.setExpression("execution(* bar())");
        System.out.println(pointcut1.matches(T1.class.getMethod("foo"), T1.class)); // false
        System.out.println(pointcut1.matches(T1.class.getMethod("bar"), T1.class)); // true

        AspectJExpressionPointcut pointcut2 = new AspectJExpressionPointcut();
        // 根据注解匹配
        pointcut2.setExpression("@annotation(org.springframework.transaction.annotation.Transactional)");
        System.out.println(pointcut2.matches(T1.class.getMethod("foo"), T1.class)); // true
        System.out.println(pointcut2.matches(T1.class.getMethod("bar"), T1.class)); // false
    }

    static class T1 {
        @Transactional
        public void foo() {}
        public void bar() {}
    }
}
```

## 2. 特殊的注解：`@Transactional`
`@Transactional`是一个在生产中很常用的注解，用来标注方法是一个事务。`@Transactional`不仅可以标注在方法上，还可以标注在一个类上，用来表示这个类的所有方法都是事务；
甚至可以标注在一个接口上，用来表示所有实现了该接口的类的所有方法都是事务。对于后两种情况，我们使用`AspectJExpressionPointcut`肯定是做不到的。

有没有一种切点是可以一次性“切”到方法、类和接口的呢？这就是`StaticMethodMatcherPointcut`一系列类所提供的功能：通过重写`match`方法，我们就能精准地识别方法、类甚至是接口上的注解。

我们首先来看一下`StaticMethodMatcherPointcut`等一系列类的继承结构。`StaticMethodMatcherPointcut`本身是一个抽象类，`match`方法继承自`StaticMethodMatcher`这个父抽象类：
```aiignore
接口        MethodMatcher                 Pointcut
                ^                           ^
                |                           |
        StaticMethodMatcher                 |
                ^                           |
                |                           |
    StaticMethodMatcherPointcut -------------
                ^
                |
       其他一些更加具体的实现类
```
对于继承了`StaticMethodMatcherPointcut`的一些更具的的实现类，其本质上也是将`match`方法进行重写。例如`NameMatchMethodPointcut`将`match`方法重写从而实现切点的名称匹配。

那么我们现在来重写一下我们的`match`方法。我们希望先从方法检测起，如果没有发现注解则检测类，再没有发现则检测其实现的接口（如果有的话），再没发现则返回`false`：
```java
StaticMethodMatcherPointcut pointcut3 = new StaticMethodMatcherPointcut() {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (method.isAnnotationPresent(Transactional.class)) {
            return true;
        } else if (targetClass.isAnnotationPresent(Transactional.class)) {
            return true;
        } else if (targetClass.getInterfaces().length != 0) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                if (anInterface.isAnnotationPresent(Transactional.class)) {
                    return true;
                }
            }
        }
        return false;
    }
};
```
可以测试一下是否正确：除了刚才已经创建了的`T1`，我们再如下创建`T2`，`T3`并让`T3`实现一个接口`I1`。
```java
public class PointcutMatching {
    public static void main(String[] params) {
        /*...*/
        System.out.println(pointcut3.matches(T1.class.getMethod("foo"), T1.class));
        System.out.println(pointcut3.matches(T2.class.getMethod("foo"), T2.class));
        System.out.println(pointcut3.matches(T2.class.getMethod("bar"), T2.class));
        System.out.println(pointcut3.matches(T3.class.getMethod("foo"), T3.class));
        System.out.println(pointcut3.matches(T3.class.getMethod("bar"), T3.class));
    }
    static class T1 {
        @Transactional
        public void foo() {}
        public void bar() {}
    }

    @Transactional
    static class T2 {
        public void foo() {}
        public void bar() {}
    }

    @Transactional
    interface I1 { void foo(); void bar();}
    static class T3 implements I1 {
        public void foo() {}
        public void bar() {}
    }
}
```
打印结果应当都是`true`：
```aiignore
true
true
true
true
true
```
当然，我们可以不自己写反射，而是采用Spring为我们提供的`MergedAnnotations`。这个类的`from`方法可以一次性获取方法和类、甚至接口上的所有注解：
```java
StaticMethodMatcherPointcut pointcut3 = new StaticMethodMatcherPointcut() {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 方法上是否加了@Transactional
        MergedAnnotations annotation = MergedAnnotations.from(method);
        if (annotation.isPresent(Transactional.class)) {
            return true;
        }
        // 类上是否加了@Transactional
        annotation = MergedAnnotations.from(targetClass);
        if (annotation.isPresent(Transactional.class)) {
            return true;
        }
        // 接口上是否加了@Transactional
        annotation = MergedAnnotations.from(targetClass.getInterfaces());
        if (annotation.isPresent(Transactional.class)) {
            return true;
        }
        return false;
    }
};
```
由于我们只给`from`方法传入了一个参数，所以默认采用的搜索策略是`SearchStrategy.DIRECT`：顾名思义，就是只查找当前等级的注解。
如果我们再给`from`指定`SearchStrategy.TYPE_HIERARCHY`，那么它就会对整个类型层次结构进行全面搜索，包括超类和已实现的接口：

```java
StaticMethodMatcherPointcut pointcut3 = new StaticMethodMatcherPointcut() {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 方法上是否加了@Transactional
        MergedAnnotations annotation = MergedAnnotations.from(method);
        if (annotation.isPresent(Transactional.class)) {
            return true;
        }
        // 类、父类或接口上是否加了@Transactional
        annotation = MergedAnnotations.from(targetClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        if (annotation.isPresent(Transactional.class)) {
            return true;
        }
        return false;
    }
};
```
同样可以打印测试一下，我们会发现都显示`true`，说明`@Transactional`注解都被完全扫描到了。