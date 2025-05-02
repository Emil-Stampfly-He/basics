# CGLIB动态代理

更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)
>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 注意：以下代码若运行报错：
> 
>>`java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int,java.security.ProtectionDomain) throws java.lang.ClassFormatError accessible: module java.base does not "opens java.lang" to unnamed module @2ef1e4fa`
>
> 则需要手动在VM Option中添加指令：`--add-opens java.base/java.lang=ALL-UNNAMED`

## 1. 反射调用：`method.invoke`
```java
public class Target {

    public void save() {
        System.out.println("save()");
    }

    public void save(int i) {
        System.out.println("save(int)");
    }

    public void save(long l) {
        System.out.println("save(long)");
    }
}
```
```java
public class $Proxy0 extends Target{

    private MethodInterceptor interceptor;
    public void setMethodInterceptor(MethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

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
```java
public class TestProxy {

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
}
```

## 2. `methodProxy`

```java
@SuppressWarnings("unused")
public class $Proxy0 extends Target{

    private MethodInterceptor interceptor;
    public void setMethodInterceptor(MethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

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
            save0Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "()V", "save", "saveOrigin");
            save1Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(I)V", "save", "saveOrigin");
            save2Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(J)V", "save", "saveOrigin");
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 原始方法，未增强
    public void saveOrigin() {super.save();}
    public void saveOrigin(int i) {super.save(i);}
    public void saveOrigin(long l) {super.save(l);}

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 带增强功能的方法
    @Override
    public void save() {
        try {
            interceptor.intercept(this, save0, new Object[0], save0Proxy);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    @Override
    public void save(int i) {
        try {
            interceptor.intercept(this, save1, new Object[]{i}, save1Proxy);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    @Override
    public void save(long l) {
        try {
            interceptor.intercept(this, save2, new Object[]{l}, save2Proxy);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}
```

## 3. `methodProxy`底层原理：`FastClass`
`MethodProxy`在生成的时候就会创建`FastClass`。
```java
save0Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "()V", "save", "saveOrigin");
save1Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(I)V", "save", "saveOrigin");
save2Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(J)V", "save", "saveOrigin");
```
具体来说，使用`methodProxy.invoke`和`methodProxy.invokeSuper`会生成两种`FastClass`的子类：`methodProxy.invoke`会生成基于目标类的`FastClass`，`methodProxy.invokeSuper`会生成基于代理类的`FastClass`。
我们首先来看`methodProxy.invoke`的原理：
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
        switch (index){
            case 0: {
                ((Target) target).save();
                return null;
            }
            case 1: {
                // save(int)需要1个参数，因此传入args[0]
                ((Target) target).save((int) args[0]);
                return null;
            }
            case 2: {
                // save(long)需要1个参数，因此传入args[0]
                ((Target) target).save((long) args[0]);
                return null;
            }
            default: throw new NoSuchMethodError("No such method!");
        }
    }
}
```

写一个测试检测一下`TargetFastClass`的行为：
```java
public static void main(String[] args) {
    TargetFastClass targetFastClass = new TargetFastClass();
    int index = targetFastClass.getIndex(new Signature("save", "()V"));
    System.out.println(index);

    targetFastClass.invoke(index, new Target(), new Object[0]); // save()

    index = targetFastClass.getIndex(new Signature("save", "(I)V"));
    targetFastClass.invoke(index, new Target(), new Object[]{1}); // save(int)
}
```

现在，我们来看`methodProxy.invokeSuper`的原理。仍然先初始化方法签名，但是我们还能使用`TargetFastClass`中的增强方法的签名吗？
```java
// TargetFastClass
save0Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "()V", "save", "saveOrigin");
save1Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(I)V", "save", "saveOrigin");
save2Proxy = MethodProxy.create($Proxy0.class.getClassLoader(), Target.class, $Proxy0.class, "(J)V", "save", "saveOrigin");
```
假设可以，那么在调用`methodProxy.invokeSuper`时，该方法会去调用增强方法：
```java
@Override
    public void save() {
        try {
            interceptor.intercept(this, save0, new Object[0], save0Proxy);
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }
    @Override
    public void save(int i) {/*...*/}
    @Override
    public void save(long l) {/*...*/}
```
增强的方法又会调用拦截器的`intercept`方法，导致传入拦截器中，再度回到`methodProxy.invokeSuper`中，形成死循环。因此我们必须调用原始方法：
```java
static Signature signature0 = new Signature("saveOriginal", "()V");
static Signature signature1 = new Signature("saveOriginal", "(I)V");
static Signature signature2 = new Signature("saveOriginal", "(J)V");
```
`invoke`也需要调用原始方法，所以同样需要修改：
```java
public Object invoke(int index, Object proxy, Object[] args) {
    switch (index) {
        case 0 -> {
            (($Proxy0) proxy).saveOrigin();
            return null;
        }
        case 1 -> {
            (($Proxy0) proxy).saveOrigin((int) args[0]);
            return null;
        }
        case 2 -> {
            (($Proxy0) proxy).saveOrigin((long) args[0]);
            return null;
        }
        default -> throw new NoSuchMethodError("No such method!");
    }
}
```