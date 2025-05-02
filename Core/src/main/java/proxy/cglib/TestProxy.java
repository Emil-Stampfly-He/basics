package proxy.cglib;

public class TestProxy {

    public static void main(String[] args) {
        $Proxy0 proxy = new $Proxy0();
        Target target = new Target();
        proxy.setMethodInterceptor((px, method, params,methodProxy) -> {
            System.out.println("before...");
            // return method.invoke(target, params); // 反射调用
            // return methodProxy.invoke(target, params); // 无反射，结合目标使用
            return methodProxy.invokeSuper(px, params); // 无反射，结合代理使用
        });

        proxy.save();
        proxy.save(1);
        proxy.save(2L);
    }
}
