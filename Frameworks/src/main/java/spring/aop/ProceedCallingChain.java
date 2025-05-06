package spring.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;

public class ProceedCallingChain {

    static class Target {
        public void foo() {
            System.out.println("Target.foo()");
        }
    }

    static class Advice1 implements MethodInterceptor {
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            System.out.println("Advice1.before()");
            Object result = methodInvocation.proceed();// 调用下一个通知或目标
            System.out.println("Advice1.after()");
            return result;
        }
    }

    static class Advice2 implements MethodInterceptor {
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            System.out.println("Advice2.before()");
            Object result = methodInvocation.proceed();// 调用下一个通知或目标
            System.out.println("Advice2.after()");
            return result;
        }
    }

    static class MyInvocation implements MethodInvocation {
        private Object target;
        private Method method;
        private Object[] args;
        private List<MethodInterceptor> interceptors;
        private int count = 1;

        public MyInvocation(Object target, Method method, Object[] args, List<MethodInterceptor> interceptors) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.interceptors = interceptors;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object[] getArguments() {
            return args;
        }

        /**
         * 调用每一个环绕通知，最后调用目标
         * @return
         * @throws Throwable
         */
        @Override
        public Object proceed() throws Throwable {
            if (count > interceptors.size()) {
                // 说明通知调用完毕，可以调用目标并结束递归
                return method.invoke(target, args);
            }
            // 逐一调用通知，调用完毕后count++
            MethodInterceptor interceptor = interceptors.get(count++ - 1);
            return interceptor.invoke(this);
        }

        @Override
        public Object getThis() {
            return target;
        }

        @Override
        public AccessibleObject getStaticPart() {
            return method;
        }

        public static void main(String[] args) throws Throwable {
            Target target = new Target();
            List<MethodInterceptor> interceptors = List.of(
                    new Advice1(),
                    new Advice2()
            );
            MyInvocation invocation = new MyInvocation(target, Target.class.getMethod("foo"), new Object[0], interceptors);
            invocation.proceed();
        }
    }
}
