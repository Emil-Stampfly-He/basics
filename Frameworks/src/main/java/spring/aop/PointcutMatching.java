package spring.aop;

import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class PointcutMatching {

    public static void main(String[] args) throws Exception {
        AspectJExpressionPointcut pointcut1 = new AspectJExpressionPointcut();
        // 根据方法名字匹配
        pointcut1.setExpression("execution(* bar())");
        System.out.println(pointcut1.matches(T1.class.getMethod("foo"), T1.class)); // false
        System.out.println(pointcut1.matches(T1.class.getMethod("bar"), T1.class)); // true

        AspectJExpressionPointcut pointcut2 = new AspectJExpressionPointcut();
        // 根据方法上注解匹配
        pointcut2.setExpression("@annotation(org.springframework.transaction.annotation.Transactional)");
        System.out.println(pointcut2.matches(T1.class.getMethod("foo"), T1.class)); // true
        System.out.println(pointcut2.matches(T1.class.getMethod("bar"), T1.class)); // false

        StaticMethodMatcherPointcut pointcut3 = new StaticMethodMatcherPointcut() {
            @Override
            public boolean matches(Method method, Class<?> targetClass) {
                MergedAnnotations annotation = MergedAnnotations.from(method);
                // 方法上是否加了@Transactional
                if (annotation.isPresent(Transactional.class)) {
                    return true;
                }

                return false;
            }
        };
    }

    static class T1 {
        @Transactional
        public void foo() {}
        public void bar() {}
    }

    @Transactional
    static class T2 {
        public void foo() {}
        public void bar() {}
    }

    interface I1 {

    }
}
