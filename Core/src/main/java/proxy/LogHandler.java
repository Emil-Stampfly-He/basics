package proxy;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LogHandler implements InvocationHandler {
    private final Object target;
    private static final Logger log = LoggerFactory.getLogger(LogHandler.class);

    public LogHandler(Object target) {this.target = target;}

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("Method {} start...", method.getName());
        Object result = method.invoke(target, args);
        log.info("Method {} end..", method.getName());

        return result;
    }
}
