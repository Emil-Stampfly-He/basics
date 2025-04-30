package proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public void addUser(String name) {
        log.info("User added: {}", name);
    }

    // 静态方法无法被动态增强
    @MethodNeeded
    public static void staticMethod(String name) { log.info("Static method: {}", name); }
}
