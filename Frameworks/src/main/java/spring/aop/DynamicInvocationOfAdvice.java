package spring.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class DynamicInvocationOfAdvice {
    @Aspect
    static class MyAspect {
        @Before("execution(* foo(..))") // 静态通知调用，不带参数绑定，执行时不需要切点
        public void before1() {
            System.out.println("before 1");
        }
        // 动态通知调用，需要参数绑定，性能更低，执行时仍需要切点
        // 静态部分在代理创建时就已经筛掉不可能的目标方法
        // 动态部分时会需要通过MethodMather.matches方法再跑一次，只有返回true才真正执行before2
        @Before("execution(* foo(..)) && args(x)")
        public void before2(int x) {
            System.out.printf("before 2 %d\n", x);;
        }
    }

    static class Target {
        public void foo(int x) {
            System.out.printf("target foo(%d)%n", x);
        }
    }

    @Configuration
    static class MyConfig {
        @Bean
        AnnotationAwareAspectJAutoProxyCreator proxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }

        @Bean
        public MyAspect myAspect() {return new MyAspect();}
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Throwable {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(ConfigurationClassPostProcessor.class);
        context.registerBean(MyConfig.class);
        context.refresh();

        AnnotationAwareAspectJAutoProxyCreator creator = context.getBean(AnnotationAwareAspectJAutoProxyCreator.class);
        Method findEligibleAdvisors = creator
                .getClass()
                .getSuperclass()
                .getSuperclass()
                .getDeclaredMethod(
                "findEligibleAdvisors", Class.class, String.class);
        findEligibleAdvisors.setAccessible(true);
        List<Advisor> list = ((List<Advisor>) findEligibleAdvisors.invoke(creator, Target.class, "target"));

        Target target = new Target();
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvisors(list);
        Target proxy = ((Target) proxyFactory.getProxy());

        List<Object> interceptorsList = proxyFactory.getInterceptorsAndDynamicInterceptionAdvice(Target.class.getMethod("foo", int.class), Target.class);
        interceptorsList.forEach(DynamicInvocationOfAdvice::showDetails);

        System.out.println();

        

    }

    public static void showDetails(Object o) {
        try {
            Class<?> clazz = Class.forName("org.springframework.aop.framework.InterceptorAndDynamicMethodMatcher");
            if (clazz.isInstance(o)) {
                Field matcher = clazz.getDeclaredField("matcher");
                Field interceptor = clazz.getDeclaredField("interceptor");
                matcher.setAccessible(true);
                interceptor.setAccessible(true);

                System.out.println("环绕通知和切点：" + o);
                System.out.println("\t切点为：" + matcher.get(o));
                System.out.println("\t通知为" + interceptor.get(o));
            } else {
                System.out.println("普通环绕通知：" + o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
