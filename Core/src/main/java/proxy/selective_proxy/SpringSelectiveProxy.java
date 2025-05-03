package proxy.selective_proxy;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

public class SpringSelectiveProxy {

    @Aspect
    static class MyAspect {
        @Before("execution (* foo())")
        public void before() {System.out.println("before myAspect");}
        @After("execution (* foo())")
        public void after() {System.out.println("after myAspect");}
    }


    public static void main(String[] params) {
        /*
        * 两个切面概念: aspect & advisor
        * aspect =
        *   advice1（通知）+ pointcut1（切点）= advisor1
        *   advice2 + pointcut2 = advisor2
        *   advice3 + pointcut3 = advisor3
        *   ...
        *
        * advisor：更细粒度的切面，包含一个通知和一个切点
        * aspect：由多个advisors组成
        * */

        // 1. 备好切点
        // 2. 备好通知
        // 3. 备好切面



    }
}
