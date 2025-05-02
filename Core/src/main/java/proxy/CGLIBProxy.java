package proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

public class CGLIBProxy {

    static class Target {
        public void foo() {System.out.println("CGLIBProxy.foo()");}
    }

    public static void main(String[] args) {
        Target target = new Target();
        Target proxy = (Target) Enhancer.create(Target.class, (MethodInterceptor) (p, method, args1, methodProxy) -> {
            System.out.println("before...");
            // Object result = method.invoke(target, args1); // 内部使用反射
            // Object result = methodProxy.invoke(target, args1); // 内部没有使用反射
            Object result = methodProxy.invokeSuper(p, args1); // 不使用反射，不需要目标
            System.out.println("after...");
            return result;
        });

        proxy.foo();
    }
}
