# JDK动态代理

>**本笔记基于黑马程序员 Spring高级源码解读**

## 1. “静态”代理：方法写死
最原始的做法，是直接手工写一个代理类，把所有要增强的逻辑卸载方法中，这就叫**静态代理**。
我们模拟创建一个代理类`$Proxy0`，这个类实际上会由JDK通过ASM的技术生成`.class`字节码并被动态地加载。
```java
public class Main {
    interface Foo {void foo();}
    static class Target implements Foo {
        @Override public void foo() {System.out.println("target foo");}
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0(); // 手写的代理类
        proxy.foo();
    }
}
```
```java
public class $Proxy0 implements Main.Foo {
    
    @Override
    public void foo() {
        System.out.println("before..."); // 增强逻辑写死
        new Main.Target().foo();
    }
}
```
很显然，写死是不行的，因为JDK在代理的时候并不知道增强的逻辑是什么。所以我们需要把代理类`$Proxy0`中的方法做成“抽象”的方法（不是`abstract`的意思），使得调用者在调用的时候能够指定具体实现。

## 2. 引入拦截器：把“增强逻”做成可配置
我们把增强逻辑抽象成一个接口`InvocationHandler`，调用方在创建代理时，传入自己的增强逻辑。这样，代理类只负责“执行”拦截器，由拦截器决定具体做什么。
```java
public interface InvocationHandler {
    void invoke(); // 只定义一个无参无返回的 invoke 方法
}
```
```java
public class Main {
    interface Foo {void foo();}
    static class Target implements Foo {
        @Override public void foo() {System.out.println("target foo");}}

    public static void main(String[] args) {
        // 在这里传入增强逻辑：先打印 before，再调用 Target.foo()
        Foo proxy = new $Proxy0(() -> {
            System.out.println("before...");
            new Target().foo();
        });
        proxy.foo(); // 交给拦截器执行
    }
}
```
```java
// 代理类不写死逻辑，把 invoke 调用交给 handler
public class $Proxy0 implements Main.Foo {
    // 使用拦截器能够获取要增强的逻辑
    private InvocationHandler handler;
    public $Proxy0(InvocationHandler handler) {this.handler = handler;}
    
    @Override public void foo() {handler.invoke();}
}
```
一个很明显的改进点是：增强逻辑由外部传入，代理类和增强逻辑解耦。

但仍有一处写死：`new Target().foo()`。如果`Target`类中还有一个`bar`方法：
```java
public class Main {

    interface Foo {
        void foo();
        void bar();
    }

    static class Target implements Foo {
        @Override
        public void foo() {
            System.out.println("target foo");
        }

        @Override
        public void bar() {
            System.out.println("target bar");
        }
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0(() -> {
            System.out.println("before...");
            new Target().foo();
        });
        proxy.bar(); // 调用bar方法
    }
}
```
我们就必须手动在拦截器里写`new Target().bar()`。能不能更加灵活、更加动态呢？

## 3. 完全动态：反射获取`Method`，自动分发
为了让拦截器知道要执行`foo()`还是`bar()`，我们把`InvocationHandler`的签名改为：
```java
public interface InvocationHandler {
    void invoke(Method method, Object[] args) throws Throwable;
}
```
这样，代理类在每个方法里都先通过反射拿到对应的`Method`对象，再传给拦截器：
```java

public class Main {

    interface Foo {
        void foo();
        void bar();
    }

    static class Target implements Foo {
        @Override public void foo() {System.out.println("target foo");}
        @Override public void bar() {System.out.println("target bar");}
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0((method, params) -> {
            System.out.println("before...");
            // 反射调用：第一个参数是目标对象，第二个参数是方法入参数组
            method.invoke(new Target(), params);
        });
        proxy.foo();
        proxy.bar();
    }
}
```
```java
public class $Proxy0 implements Main.Foo {
    private final InvocationHandler handler;
    public $Proxy0(InvocationHandler handler) {this.handler = handler;}

    @Override
    public void foo() {
        try {
            Method foo = Main.Foo.class.getMethod("foo");
            handler.invoke(foo, new Object[]{});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void bar() {
        try {
            Method bar = Main.Foo.class.getMethod("bar");
            handler.invoke(bar, new Object[]{});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
```
当然，由于这里的`foo`方法和`bar`方法是没有参数的，所以这里的参数数组直接传入`new Object[0]`。

这样，无论接口有多少方法，代理类模板都一样：通过反射拿到`Method`，把方法名、参数、调用都交给统一的`InvocationHandler`。
并且，拦截器既可以在调用前后做任何增强，也可以在运行时决定要调用哪个目标对象的哪个方法，真正做到了“全动态”。

总结一下JDK动态代理的调用链：
```aiignore
[客户端调用 proxy.foo()]
            ↓
[$Proxy0.foo() 方法体]
            ↓
handler.invoke(proxy, fooMethod, args)
            ↓
[用户在 handler 里写的增强逻辑]
(before...；method.invoke(target))
            ↓
[Target.foo() 真正的业务方法]
```