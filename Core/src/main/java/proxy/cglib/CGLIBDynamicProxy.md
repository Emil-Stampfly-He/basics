# CGLIB动态代理

更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)
>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 注意：以下代码若运行报错：
> 
>>`java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @2ef1e4fa`
>
> 则需要手动在VM Option中添加指令：`--add-opens java.base/java.lang=ALL-UNNAMED`

与JDK动态代理技术类似，CGLIB也是一款基于 ASM 字节码生成的高性能代理框架。
但是，它通过“继承目标类＋重写目标方法”的方式，在运行时动态生成一个子类来完成代理。因此代理对象与实例对象不是JDK代理中的平级关系。


## 1. 纯反射实现：`method.invoke`
在深入 CGLIB 之前，我们先来看一下最原始的“纯反射”版本，理解它的优缺点。

首先定义一个很简单的目标类 `Target`，它有三个重载的 `save` 方法：无参、有一个 `int`、有一个 `long`。  
拦截器`MethodInterceptor`我们就i直接使用CGLIB给我们提供的实现，它在每次代理方法被调用时都会被触发。

```java
public class Target {
    public void save() {System.out.println("save()");}
    public void save(int i) {System.out.println("save(int)");}
    public void save(long l) {System.out.println("save(long)");}
}
```
由于CGLIB代理类是目标类的子类，所以我们需要代理类去继承目标类。另外有三个注意的点：
1. 我们在静态代码块中初始化`Target.class.getMethod(...)`得到的`Method`对象，避免每次调用时都做一次高成本的反射查找
2. 在`save()`、`save(int)`、`save(long)`中，只做一件事：把当前代理实例、`Method`对象和参数数组，交给`MethodInterceptor`
3. `inteceptor.intercept()`方法的第四个参数是`methodProxy`，这个参数目前先不管，传入`null`
```java
public class $Proxy0 extends Target{

    private MethodInterceptor interceptor;
    public void setMethodInterceptor(MethodInterceptor interceptor) {this.interceptor = interceptor;}

    // 1. 初始化三个重载 save 方法的 Method 对象
    static Method save0;
    static Method save1;
    static Method save2;
    static {
        try {
            save0 = Target.class.getMethod("save");
            save1 = Target.class.getMethod("save", int.class);
            save2 = Target.class.getMethod("save", long.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    // 2. 重写无参 save，所有逻辑都交给 interceptor
    @Override
    public void save() {
        try {
            interceptor.intercept(this, save0, new Object[0], null);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public void save(int i) {
        try {
            // 3. methodProxy传入null
            interceptor.intercept(this, save1, new Object[]{i}, null);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public void save(long l) {
        try {
            interceptor.intercept(this, save2, new Object[]{l}, null);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
```
我们写一个测试来看看代理类的行为：
```java
public static void main(String[] args) {
    $Proxy0 proxy = new $Proxy0();
    Target target = new Target();
    proxy.setMethodInterceptor((o, method, params,methodProxy) -> {
        System.out.println("before...");
        return method.invoke(target, params); // 反射调用
    });

    proxy.save();
    proxy.save(1);
    proxy.save(2L);
}
```
总结一下调用链：
`proxy.save()` → 进入代理类的重写方法 → 调用拦截器`interceptor.intercept()` → 拦截器执行前置打印后反射调用方法


## 2. `methodProxy`

### 2.1. 什么是`MethodProxy`
CGLIB在生成代理类时，会为每个拦截的方法同时生成一个`MethodProxy`，它内部实际上就是桥接到目标（或代理）类的特定方法调用点。
调用`methodProxy.invoke`或`invokeSuper`时，并不反射地调用，而是调用CGLIB自己生成“快速调用”逻辑，其性能比原来的反射调用方法要高。

### 2.2. 代理类改造
在静态块中，除了缓存`Method`，还要`create`出对应的`MethodProxy`。`MethodProxy.create`方法需要传递6个参数，分为4个部分
1. 一个类加载器，直接使用代理类自己的类加载就可以
2. 目标类和代理类的`class`
3. 增强方法签名：方法名称 + 方法参数类型 + 方法返回值类型
4. 原始方法的方法名

因为要用到原始方法，所以我们还必须额外再创建出三个`save`方法的未增强（即原始）版。我们可以给它们命名为`saveOrignal`，直接使用`super`调用父类`Target`中的逻辑即可：
```java
public class $Proxy0 extends Target {
    /*...*/
    static Method save0;
    static Method save1;
    static Method save2;
    static MethodProxy save0Proxy;
    static MethodProxy save1Proxy;
    static MethodProxy save2Proxy;

    static {
        try {
            save0 = Target.class.getMethod("save");
            save1 = Target.class.getMethod("save", int.class);
            save2 = Target.class.getMethod("save", long.class);
            // ()V表示方法无参数，返回值类型为void
            save0Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "()V", "save", "saveOrigin");
            // I表示int
            save1Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(I)V", "save", "saveOrigin");
            // J表示long
            save2Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(J)V", "save", "saveOrigin");
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 原始方法，未增强 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    public void saveOrigin() {super.save();}
    public void saveOrigin(int i) {super.save(i);}
    public void saveOrigin(long l) {super.save(l);}

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 带增强功能的方法 <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
    @Override
    public void save() {/*...*/}
    @Override
    public void save(int i) {/*...*/}
    @Override
    public void save(long l) {/*...*/}
}
```
这样，我们就能够使用`methodProxy`从而避开反射调用了。我们可以分别使用`methodProxy.invoke()`和`invokeSuper`方法来测试一下。
两种方法所展现的最终结果应当是一致的，尽管传递的参数不同、底层原理不同，有关细节我们会在下一节具体讨论。现在只需要知道，两种方法都不使用反射即可。
```java
// 将method.invoke(target, params)改成
return methodProxy.invoke(target, params); // 无反射，结合目标使用
return methodProxy.invokeSuper(px, params); // 无反射，结合代理使用
```
到此为止，我们的代理类就实现完毕了。


## 3. `MethodProxy`底层原理：`FastClass`
上节提到，尽管`methodProxy.invoke`和`invokeSuper`的行为是一致的，但是方法签名不同，底层原理也不同。现在让我们详细探讨相关细节。

首先说明结论：CGLIB在生成`MethodProxy`时会为目标类和代理类分别生成一个`FastClass`子类，并通过子类对象直接调用方法（而不是反射）。
其中，目标类`FastClass`对应`methodProxy.invoke`的底层实现，代理类`FastClass`对应`methodProxy.invokeSuper`的底层实现。这可以从方法传入的参数看出：
* `invoke`传入的是**目标类对象**和待增强方法参数
* `invokeSuper`传入的是**代理对象**和待增强方法参数

这一点不同于JDK动态代理：JDK动态代理在调用`method.invoke`时是通过反射调用的，只有调用超过16次才会生成一个新的代理类对象，进行非反射调用增强性能。

`FastClass`有两个最重要的`abstract`方法：`getIndex`和`invoke`。
1. `getIndex` \
需要传给该方法一个`Signature`对象，即方法签名。然后给方法加上索引，返回索引传给`invoke`方法调用。
2. `invoke` \
接受`getIndex`传过来的索引，接受并直接使用一个目标类对象，通过接受参数数组来直接调用方法。

有了这两个方法，`FastClass`就能精确地知道需要调用哪个方法。当然，`FastClass`本身作为父类，它两种方法都没有具体实现，而是交给目标类`FastClass`和代理类`FastClass`两个子类去实现的。
接下来就让我们重写这两个最重要的`abstract`方法。

> 接下来，为了方便称呼，令目标类的`FastClass`子类为`TargetFastClass`，代理类的`FastClass`子类为`ProxyFastClass`。
> 由于`FastClass`中有好几个`abstract`方法，为了方便，我们不让`TargetFastClass`和`ProxyFastClass`继承`FastClass`。
> 我们只要知道现实中这两个自动生成的类确实会继承`FastClass`并重写其未实现的`abstract`方法就好。

### 3.1. 目标类`FastClass`：`TargetFastClass`
由于三种`save`方法有三种签名，所以需要初始化三个`Signature`对象。`Signature`构造器的第一个参数是方法的名称，第二个参数是方法的参数类型与返回值类型：
```java
static Signature signature0 = new Signature("save", "()V"); // ()V表示方法无参数，返回值类型未void
static Signature signature1 = new Signature("save", "(I)V"); // I表示方法有一个int参数
static Signature signature2 = new Signature("save", "(J)V"); // J表示方法有一个long参数
```

然后是`getIndex`方法。我们可以用`if-else`给三种`save`方法从0到2编上号：
```java
/**
 * 获取目标方法的编号
 * Target
 *     save()        0
 *     save(int)     1
 *     save(long)    2
 * @param signature 方法签名，包含方法名，方法参数和方法返回类型
 * @return 方法编号
 */
public int getIndex(Signature signature){
    if (signature.equals(signature0)){
        return 0;
    } else if (signature.equals(signature1)) {
        return 1;
    }  else if (signature.equals(signature2)) {
        return 2;
    } else {
        return -1;
    }
}
```
最后是`invoke`方法。传入方法编号、目标类对象和方法参数，让对象直接根据参数调用方法：
```java
public Object invoke(int index, Object target, Object[] args) {
    return switch (index) {
        case 0 -> {
            ((Target) target).save();
            yield null;
        }
        case 1 -> {
            // save(int)需要1个参数，因此传入args[0]
            ((Target) target).save((int) args[0]);
            yield null;
        }
        case 2 -> {
            // save(long)需要1个参数，因此传入args[0]
            ((Target) target).save((long) args[0]);
            yield null;
        }
        default -> throw new NoSuchMethodError("No such method!");
    };
}
```
> 这里使用了Java高版本的语法，实际上就是普通的`switch`模式匹配。

最终的`TargetFastClass`类完整代码如下：
```java
public class TargetFastClass {

    static Signature signature0 = new Signature("save", "()V");
    static Signature signature1 = new Signature("save", "(I)V");
    static Signature signature2 = new Signature("save", "(J)V");

    /**
     * 获取目标方法的编号
     * Target
     *     save()        0
     *     save(int)     1
     *     save(long)    2
     * @param signature 方法签名，包含方法名，方法参数和方法返回类型
     * @return 方法编号
     */
    public int getIndex(Signature signature){
        if (signature.equals(signature0)){
            return 0;
        } else if (signature.equals(signature1)) {
            return 1;
        }  else if (signature.equals(signature2)) {
            return 2;
        } else {
            return -1;
        }
    }

    public Object invoke(int index, Object target, Object[] args) {
        return switch (index) {
            case 0 -> {
                ((Target) target).save();
                yield null;
            }
            case 1 -> {
                // save(int)需要1个参数，因此传入args[0]
                ((Target) target).save((int) args[0]);
                yield null;
            }
            case 2 -> {
                // save(long)需要1个参数，因此传入args[0]
                ((Target) target).save((long) args[0]);
                yield null;
            }
            default -> throw new NoSuchMethodError("No such method!");
        };
    }
}
```
写一个测试检测一下`TargetFastClass`的行为：
```java
public static void main(String[] args) {
    TargetFastClass targetFastClass = new TargetFastClass();
    int index = targetFastClass.getIndex(new Signature("save", "()V")); // 0
    System.out.println(index);

    targetFastClass.invoke(index, new Target(), new Object[0]); // save()

    index = targetFastClass.getIndex(new Signature("save", "(I)V"));
    targetFastClass.invoke(index, new Target(), new Object[]{1}); // save(int)
}
```
输出确实符合我们的预期：
```aiignore
0
save()
save(int)
```

### 3.2. 代理类`FastClass`：`ProxyFastClass`
`ProxyFastClass`跟`TargetFastClass`只有一点不同：需要调用未被增强的原始方法：
```java
static Signature signature0 = new Signature("saveOriginal", "()V");
static Signature signature1 = new Signature("saveOriginal", "(I)V");
static Signature signature2 = new Signature("saveOriginal", "(J)V");

public int getIndex(Signature signature) {/*...*/}

public Object invoke(int index, Object proxy, Object[] args) {
    return switch (index) {
        case 0 -> {
            (($Proxy0) proxy).saveOrigin();
            yield null;
        }
        case 1 -> {
            (($Proxy0) proxy).saveOrigin((int) args[0]);
            yield null;
        }
        case 2 -> {
            (($Proxy0) proxy).saveOrigin((long) args[0]);
            yield null;
        }
        default -> throw new NoSuchMethodError("No such method!");
    };
}
```
为什么不是调用增强方法？回顾一下`methodProxy`的调用：
```java
proxy.setMethodInterceptor((px, method, params,methodProxy) -> {
        System.out.println("before...");
        // return methodProxy.invoke(target, params); // 无反射，结合目标使用
        return methodProxy.invokeSuper(px, params); // 无反射，结合代理使用
    });
```
假设我们就调用增强方法，那么方法的调用链是：

`proxyFastClass.invoke`（表层是`methodProxy.invokeSuper`） → `proxy.save`（增强了的`save`）→ 被拦截器`interceptor.intercept`增强 → `methodProxy.invokeSuper` → 
被拦截器`interceptor.intercept`增强 → `methodProxy.invokeSuper`...

这是一个死循环，显然不正确。所以我们需要调用原始（未增强）的方法：
```java
public class ProxyFastClass {

    static Signature signature0 = new Signature("saveOriginal", "()V");
    static Signature signature1 = new Signature("saveOriginal", "(I)V");
    static Signature signature2 = new Signature("saveOriginal", "(J)V");

    /**
     * 获取代理方法的编号
     * Proxy
     *     save()        0
     *     save(int)     1
     *     save(long)    2
     * @param signature 方法签名，包含方法名，方法参数和方法返回类型
     * @return 方法编号
     */
    public int getIndex(Signature signature) {
        if (signature.equals(signature0)) {
            return 0;
        } else if (signature.equals(signature1)) {
            return 1;
        } else if (signature.equals(signature2)) {
            return 2;
        } else {
            return -1;
        }
    }

    public Object invoke(int index, Object proxy, Object[] args) {
        return switch (index) {
            case 0 -> {
                (($Proxy0) proxy).saveOrigin();
                yield null;
            }
            case 1 -> {
                (($Proxy0) proxy).saveOrigin((int) args[0]);
                yield null;
            }
            case 2 -> {
                (($Proxy0) proxy).saveOrigin((long) args[0]);
                yield null;
            }
            default -> throw new NoSuchMethodError("No such method!");
        };
    }
}
```
最后需要注意的是，`invoke`使用代理进行方法的调用，因为这是代理类的`FastClass`。

我们可以测试一下`ProxyFastClass`的行为是否符合预期：
```java
public static void main(String[] args) {
    ProxyFastClass proxyFastClass = new ProxyFastClass();
    int index = proxyFastClass.getIndex(new Signature("saveOriginal", "()V"));
    proxyFastClass.invoke(index, new $Proxy0(), new Object[0]); // save()

    index = proxyFastClass.getIndex(new Signature("saveOriginal", "(I)V"));
    proxyFastClass.invoke(index, new $Proxy0(), new Object[]{100}); // save(int)
}
```
输出如下，确实没有问题：
```aiignore
save()
save(int)
```