package spring.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AspectToAdvisor {

    static class Aspect {

        @Before("execution(* foo())")
        public void before1() {
            System.out.println("before1");
        }

        @Before("execution(* foo())")
        public void before2() {
            System.out.println("before2");
        }

        public void after() {
            System.out.println("after");
        }
        @AfterReturning("execution(* foo())")
        public void afterReturning() {
            System.out.println("afterReturning");
        }

        public void afterThrowing() {
            System.out.println("afterThrowing");
        }
        @Around("execution(* foo())")
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            System.out.println("around before");
            Object result = pjp.proceed();
            System.out.println("around after");
            return result;
        }

        static class Target {
            public void foo() {
                System.out.println("Target foo");
            }
        }
    }

    // 1. 高级切面转换为低级切面
    public static void main(String[] args) throws Throwable {
        AspectInstanceFactory factory = new SingletonAspectInstanceFactory(new Aspect());
        List<Advisor> list = new ArrayList<>();
        for (Method method: Aspect.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Before.class)) {
                // 解析切点
                Before before = method.getAnnotation(Before.class);
                assert before != null;
                String pointcutExpression = before.value();
                AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                pointcut.setExpression(pointcutExpression);
                // 通知类
                AspectJMethodBeforeAdvice beforeAdvice = new AspectJMethodBeforeAdvice(method, pointcut, factory);
                // advisor
                Advisor advisor = new DefaultPointcutAdvisor(pointcut,  beforeAdvice);
                list.add(advisor);
            } else if (method.isAnnotationPresent(Around.class)) {
                // 解析切点
                Around around = method.getAnnotation(Around.class);
                assert around != null;
                String pointcutExpression = around.value();
                AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                pointcut.setExpression(pointcutExpression);
                // 通知类
                AspectJAroundAdvice beforeAdvice = new AspectJAroundAdvice(method, pointcut, factory);
                // advisor
                Advisor advisor = new DefaultPointcutAdvisor(pointcut, beforeAdvice);
                list.add(advisor);
            } else if (method.isAnnotationPresent(AfterReturning.class)) {
                // 解析切点
                AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
                assert afterReturning != null;
                String pointcutExpression = afterReturning.value();
                AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
                pointcut.setExpression(pointcutExpression);
                // 通知类
                AspectJAfterReturningAdvice beforeAdvice = new AspectJAfterReturningAdvice(method, pointcut, factory);
                // advisor
                Advisor advisor = new DefaultPointcutAdvisor(pointcut, beforeAdvice);
                list.add(advisor);
            }
        }

        list.forEach(System.out::println);

        System.out.println();

        // 2. 通知统一转换为环绕通知MethodInterceptor
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(new Aspect.Target()); // 设置目标类
        proxyFactory.setInterfaces(Aspect.Target.class.getInterfaces()); // 设置目标类接口

        // 5. 加入最外层环绕通知
        // 准备把MethodInvocation放入当前线程
        proxyFactory.addAdvice(ExposeInvocationInterceptor.INSTANCE);
        proxyFactory.addAdvisors(list); // 加入advisors（低级切面）

        // 3. 统一转换成环绕通知
        List<Object> methodInterceptorList = proxyFactory.getInterceptorsAndDynamicInterceptionAdvice(Aspect.Target.class.getMethod("foo"), Aspect.Target.class);
        methodInterceptorList.forEach(System.out::println);

        System.out.println();

        // 4. 创建并执行调用链（环绕通知 + 目标）
        // 由于构造器是protected，所以必须反射地调用构造器
        Constructor<ReflectiveMethodInvocation> reflectiveMethodInvocationConstructor = ReflectiveMethodInvocation.class.getDeclaredConstructor(
                Object.class,
                Object.class,
                Method.class,
                Object[].class,
                Class.class,
                List.class
        );
        reflectiveMethodInvocationConstructor.setAccessible(true);
        ReflectiveMethodInvocation methodInvocation = reflectiveMethodInvocationConstructor.newInstance(
                null,
                new Aspect.Target(),
                Aspect.Target.class.getMethod("foo"),
                new Object[0],
                Aspect.Target.class,
                methodInterceptorList
        );

        // 5. 开始调用，必须将methodInvocation放入当前线程
        // 只有这样才能通知才能找到调用链
        // 这个本质上也还是一个环绕通知，只不过是最外层的环绕通知
        // ExposeInvocationInterceptor，本质上使用的是ThreadLocal
        methodInvocation.proceed(); // 内部使用递归
    }
}
