package spring.aop;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
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
                // 方法上是否加了@Transactional
                MergedAnnotations annotation = MergedAnnotations.from(method);
                if (annotation.isPresent(Transactional.class)) {
                    return true;
                }

                // 类上是否加了@Transactional
                // 加上TYPE_HIERARCHY与下面直接检测接口是否有@Transactional可以得到相同的结果
                annotation = MergedAnnotations.from(targetClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
                if (annotation.isPresent(Transactional.class)) {
                    return true;
                }

                // 接口上是否加了@Transactional
//                annotation = MergedAnnotations.from(targetClass.getInterfaces());
//                if (annotation.isPresent(Transactional.class)) {
//                    return true;
//                }
                return false;

//                if (method.isAnnotationPresent(Transactional.class)) {
//                    return true;
//                } else if (targetClass.isAnnotationPresent(Transactional.class)) {
//                    return true;
//                } else if (targetClass.getInterfaces().length != 0) {
//                    Class<?>[] interfaces = targetClass.getInterfaces();
//                    for (Class<?> anInterface : interfaces) {
//                        if (anInterface.isAnnotationPresent(Transactional.class)) {
//                            return true;
//                        }
//                    }
//                }
//
//                return false;
            }
        };

        System.out.println();
        // 以下均为true
        System.out.println(pointcut3.matches(T1.class.getMethod("foo"), T1.class));
        System.out.println(pointcut3.matches(T2.class.getMethod("foo"), T2.class));
        System.out.println(pointcut3.matches(T2.class.getMethod("bar"), T2.class));
        System.out.println(pointcut3.matches(T3.class.getMethod("foo"), T3.class));
        System.out.println(pointcut3.matches(T3.class.getMethod("bar"), T3.class));
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

    @Transactional
    interface I1 { void foo(); void bar();}
    static class T3 implements I1 {
        public void foo() {}
        public void bar() {}
    }
}
