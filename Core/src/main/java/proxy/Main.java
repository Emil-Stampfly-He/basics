package proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        UserService realObject = new UserServiceImpl();
        UserService proxyObject = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class<?>[]{UserService.class},
                new LogHandler(realObject)
        );

        proxyObject.addUser("Alice");
        UserServiceImpl.staticMethod("Static method called");

        log.info("Real Object: {}", realObject);
        log.info("Proxy Object: {}", proxyObject);

        log.info("Real Object == Proxy Object? {}", (realObject == proxyObject));
        log.info("proxyObject.getClass() = {}", proxyObject.getClass().getName());
    }
}
