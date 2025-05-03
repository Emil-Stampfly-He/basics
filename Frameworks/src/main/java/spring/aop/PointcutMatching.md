
## 1. 两种切点匹配模式

```java
public class PointcutMatching {

    public static void main(String[] args) throws Exception {
        AspectJExpressionPointcut pointcut1 = new AspectJExpressionPointcut();
        // 根据方法名字匹配
        pointcut1.setExpression("execution(* bar())");
        System.out.println(pointcut1.matches(T1.class.getMethod("foo"), T1.class)); // false
        System.out.println(pointcut1.matches(T1.class.getMethod("bar"), T1.class)); // true

        AspectJExpressionPointcut pointcut2 = new AspectJExpressionPointcut();
        // 根据注解匹配
        pointcut2.setExpression("@annotation(org.springframework.transaction.annotation.Transactional)");
        System.out.println(pointcut2.matches(T1.class.getMethod("foo"), T1.class)); // true
        System.out.println(pointcut2.matches(T1.class.getMethod("bar"), T1.class)); // false
    }

    static class T1 {
        @Transactional
        public void foo() {}
        public void bar() {}
    }
}
```

## 2. 特殊的注解：`@Transactional`
