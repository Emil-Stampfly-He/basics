package spring.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class AdvisorAndAspect {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("aspect1", Aspect1.class);
        context.registerBean("config", Config.class);
        context.registerBean(ConfigurationClassPostProcessor.class);

        // 能识别高级切面中的注解，例如@Aspect，@Before
        // 能根据收集的切面自动创建代理
        context.registerBean(AnnotationAwareAspectJAutoProxyCreator.class);
        // AnnotationAwareAspectJAutoProxyCreator实现了BeanPostProcessor接口
        // 创建 ->（*）依赖注入 -> 初始化（*）

        context.refresh();
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println(name);
        }

        System.out.println();

        /* findEligibleAdvisors() */
        AnnotationAwareAspectJAutoProxyCreator creator = context.getBean(AnnotationAwareAspectJAutoProxyCreator.class);
        Method findEligibleAdvisors = creator
                .getClass()
                .getSuperclass()
                .getSuperclass()
                .getDeclaredMethod("findEligibleAdvisors",Class.class, String.class);
        findEligibleAdvisors.setAccessible(true);
        List<Advisor> advisors = (List<Advisor>) findEligibleAdvisors.invoke(creator, Target2.class, "target2");
        advisors.forEach(System.out::println);

        System.out.println();

        /* wrapIfNecessary */
        Method wrapIfNecessary = creator.getClass()
                .getSuperclass()
                .getSuperclass()
                .getSuperclass()
                .getDeclaredMethod("wrapIfNecessary", Object.class, String.class, Object.class);
        wrapIfNecessary.setAccessible(true);
        Object o1 = wrapIfNecessary.invoke(creator, new Target1(), "target1", "target1");
        Object o2 = wrapIfNecessary.invoke(creator, new Target2(), "target2", "target2");
        System.out.println(o1.getClass());
        System.out.println(o2.getClass());

        ((Target1) o1).foo();
    }

    static class Target1 {
        public void foo() {
            System.out.println("target1 foo");
        }
    }

    static class Target2 {
        public void bar() {
            System.out.println("target2 bar");
        }
    }

    @Aspect
    static class Aspect1 {
        @Before("execution(* foo())")
        public void before() {
            System.out.println("aspect1 before...");
        }
        @After("execution(* foo())")
        public void after() {
            System.out.println("aspect1 after...");
        }
    }

    @Configuration
    static class Config { // 低级切面
        @Bean // advice需要作为bean传入
        public Advisor advisor3(MethodInterceptor advice3) {
            AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
            pointcut.setExpression("execution(* foo());");
            return new DefaultPointcutAdvisor(pointcut, advice3);
        }

        @Bean
        public MethodInterceptor advice3() {
            return invocation -> {
                System.out.println("advice3 before... ");
                Object result = invocation.proceed();
                System.out.println("advice3 after...");
                return result;
            };
        }
    }
}
