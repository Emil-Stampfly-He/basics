package spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJMethodBeforeAdvice;
import org.springframework.aop.aspectj.SingletonAspectInstanceFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

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

        public void afterReturning() {
            System.out.println("afterReturning");
        }

        public void afterThrowing() {
            System.out.println("afterThrowing");
        }

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

        // 高级切面转换为低级切面
        public static void main(String[] args) {
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
                }
            }

            list.forEach(System.out::println);
        }
    }
}
