package spring.aop.summarization;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class MyAspect {
    @Before("execution(* spring.aop.summarization.Target.foo(..))")
    public void beforeAdvice(JoinPoint joinPoint) {
        log.info("Foo is ready to do something...");
    }

    @Around("execution(* spring.aop.summarization.Target.bar(..))")
    public Object executionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Bar is ready to do something...");
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long end = System.currentTimeMillis();
        log.info("Bar execution time: {}", end - start);

        return result;
    }
}