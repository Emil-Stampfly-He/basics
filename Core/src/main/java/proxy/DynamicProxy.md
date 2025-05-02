# 动态代理、编译期织入与类加载织入

## 1. 动态代理：JDK & CGLIB
### 1.1. JDK
JDK是官方自己提供的动态代理库，只能针对接口进行动态代理。

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JDKProxy {

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
        Target target = new Target();
        Foo proxy = (Foo) Proxy.newProxyInstance(
                JDKProxy.class.getClassLoader(),
                new Class<?>[]{Foo.class},
                (proxy, method, args1) -> {
                    System.out.println("before..."); // 前置增强
                    Object result = method.invoke(target, args1); // 调用方法
                    System.out.println("after..."); // 后置增强
                    
                    return result;
                });

        proxy.foo(); // 应该会打印三行信息
    }
}
```
代理对象`proxy`与实例对象是平级关系，不存在继承关系，因此两者之间无法互相强制转换。
这同时也意味着，即使目标类是`final`类，也能够正常生成代理对象`proxy`（因为不存在继承关系）。
```java
static final class Target implements Foo {} // 也能生成代理对象
```

### 1.2 CGLIB
CGLIB动态代理不同于JDK动态代理，其可以代理一个具体的类。但是生成的代理对象是实例对象的子对象，存在继承关系。
因此代理类不允许是`final`类，且代理类无法调用代理类的`final`方法。

CGLIB另一个比较特别的点是，在代理对象调用方法时，可以使用三种调用方式：
1. `method.invoke`
2. `methodProxy.invoke`
3. `methodProxy.invokeSuper`

三种方式的行为是一致的，但是内部逻辑有些许差异：
1. `method.invoke`使用了反射来调用方法，性能会稍微差一点
2. `methodProxy.invoke`不使用反射调用方法，且不需要目标类的实例。这个方法被Spring所采用
3. `methodProxy.invokeSuper`最快，不会出发拦截器

```java
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

public class CGLIBProxy {

    // 不得是final class
    static class Target {
        // 不得是final method
        public void foo() {
            System.out.println("CGLIB foo");
        }
    }

    public static void main(String[] params) {
        Target target = new Target();
        Target proxy = (Target) Enhancer.create(Target.class, (MethodInterceptor) (p, method, args, methodProxy) -> {
            System.out.println("before..."); // 前置增强
            // Object result = method.invoke(target, args); // 使用反射，性能会弱一点
            // Object result = methodProxy.invoke(target, args); // 不使用反射 -> Spring用的
            Object result = methodProxy.invokeSuper(p, args); // 不使用反射，不需要目标，代理自己
            System.out.println("after..."); // 后置增强
            return result;
        });

        proxy.foo();
    }
}
```

## 2. 编译期织入：`AspectJ`
编译期织入（CTW），顾名思义，就是在编译成字节码的时候就对方法进行了增强。


## 3. 类加载织入：agent
