package proxy.selective_proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;


public class SpringSelectiveProxy {

    // Spring中我们这样装配AOP配置
//    @Aspect
//    static class MyAspect {
//        @Before("execution (* foo())")
//        public void before() {System.out.println("before myAspect");}
//        @After("execution (* foo())")
//        public void after() {System.out.println("after myAspect");}
//    }


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

        // 1. 备好切点: pointcut
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution (* foo(..))"); // 只增强foo方法
        // 2. 备好通知: advice
        MethodInterceptor advice = invocation -> {
            System.out.println("before myAspect");
            Object result = invocation.proceed();
            System.out.println("after myAspect");
            return result;
        };
        // 3. 备好切面: advisor = advice + pointcut
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, advice);

        /* 4. 创建代理: 自动选择使用JDK代理还是CGLIB
        * proxyTargetClass = false && 目标实现接口：JDK
        * proxyTargetClass = false && 目标未实现接口：CGLIB
        * proxyTargetClass = true：CGLIB
        * */
        ProxyFactory factory = new ProxyFactory();
        Target1 target1 = new Target1();
        factory.setTarget(target1); // 设置目标类
        factory.addAdvisor(advisor); // 绑定切面
        factory.setInterfaces(target1.getClass().getInterfaces());

        I1 proxy = (I1) factory.getProxy(); // 获得代理对象
        System.out.println(proxy.getClass().getName()); // 使用CGLIB代理

        proxy.foo(); // 被增强
        proxy.bar(); // 未被增强
    }

    interface I1 {
        void foo(); void bar();
    }

    static class Target1 implements I1 {
        public void foo() {System.out.println("target1 foo");}
        public void bar() {System.out.println("target1 bar");}
    }

    static class Target2 {
        public void foo() {System.out.println("target2 foo");}
        public void bar() {System.out.println("target2 bar");}
    }
}
