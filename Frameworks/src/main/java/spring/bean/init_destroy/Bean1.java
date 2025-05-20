package spring.bean.init_destroy;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class Bean1 implements InitializingBean {
    @PostConstruct
    public void init1() {
        log.info("init 1");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("init 2");
    }

    public void init3() {
        log.info("init 3");
    }
}
