# JDK动态代理

## 1. 最简单的情况
```java
public class Main {

    interface Foo {
        void foo();
    }

    static class Target implements Foo {
        @Override
        public void foo() {
            System.out.println("target foo");
        }
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0();
        proxy.foo();
    }
}
```

```java
public class $Proxy0 implements Main.Foo {
    
    @Override
    public void foo() {
        System.out.println("before...");
        new Main.Target().foo();
    }
}
```
很显然，写死是不行的，因为JDK在代理的时候并不知道增强的逻辑是什么。所以我们需要把代理类`$Proxy0`中的方法做成“抽象”的方法（不是`abstract`的意思），使得调用者在调用的时候能够指定具体实现。

我们可以定义一个拦截器`InvocationHandler`，当用户指定了拦截器并在拦截器中指定具体逻辑后，拦截器会回调代理类中的`foo`方法，并在那里进行增强：
```java
public interface InvocationHandler {

    void invoke(); // 为了简单，先定义为void方法
}
```
```java
public class Main {

    interface Foo {
        void foo();
    }

    static class Target implements Foo {
        @Override
        public void foo() {
            System.out.println("target foo");
        }
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0(() -> {
            System.out.println("before...");
            new Target().foo();
        });
        proxy.foo();
    }
}
```
```java
public class $Proxy0 implements Main.Foo {
    
    // 使用拦截器能够获取要增强的逻辑
    private InvocationHandler handler;

    public $Proxy0(InvocationHandler handler) {
        this.handler = handler;
    }

    @Override
    public void foo() {
        handler.invoke();
    }
}
```
调用链：要增强的逻辑通过`InvocationHandler`传给`proxy`，`proxy`中的`foo`方法被增强，

这样，我们需要增强的逻辑就从在`foo`方法中写死的状态变成可以灵活地传给一个拦截器，并动态地增强了。但问题是，我们的逻辑仍然有一部分是写死的：
```java
new Target.foo();
```
如果`Target`类中还有一个`bar`方法：
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
那么输出仍然会是`foo`方法中的逻辑。当然，我们可以在拦截器中传入调用`bar`方法的逻辑`new Target.bar()`，但这样做并不灵活，能不能自动识别我们需要增强的是哪个方法呢？
自然地，我们可以想到：反射地获取方法对象。这样，在调用`proxy.xxx()`的时候，拦截器能够自动识别到对象的方法，并调用相应的方法。
```java
public interface InvocationHandler {
    void invoke(Method method, Object[] args) throws Throwable;
}
```
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
        Foo proxy = new $Proxy0((method, params) -> {
            System.out.println("before...");
            method.invoke(new Target(), params); // 动态地识别回调方法
        });
        proxy.foo();
        proxy.bar();
    }
}
```
```java
public class $Proxy0 implements Main.Foo {

    private final InvocationHandler handler;

    public $Proxy0(InvocationHandler handler) {
        this.handler = handler;
    }

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

目前的调用链是：`InvocationHandler`对象传入`proxy`中，使用`invoke`方法回调代理类中的抽象方法。
抽象方法将自己的`Method`对象传入`invoke`中，因此`InvocationHandler`知道应该调用什么方法，因此使用`proxy`调用方法`proxy.xxx()`时能够准确识别到对应方法`xxx()`。