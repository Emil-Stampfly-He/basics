package proxy.cglib;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

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
