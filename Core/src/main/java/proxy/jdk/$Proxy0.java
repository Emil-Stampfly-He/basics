package proxy.jdk;

import java.lang.reflect.Method;

public class $Proxy0 implements Main.Foo {

    private final InvocationHandler handler;

    static Method foo;
    static Method bar;
    static {
        try {
            foo = Main.Foo.class.getMethod("foo");
            bar = Main.Foo.class.getMethod("bar");
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
    }

    public $Proxy0(InvocationHandler handler) {
        this.handler = handler;
    }

    @Override
    public void foo() {
        try {
            handler.invoke(this, foo, new Object[]{});
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int bar() {
        try {
            Object result = handler.invoke(this, bar, new Object[]{});
            return (int) result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }



}
