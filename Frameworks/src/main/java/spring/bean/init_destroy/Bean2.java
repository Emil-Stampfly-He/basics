package spring.bean.init_destroy;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

@Slf4j
public class Bean2 implements DisposableBean {
    @PreDestroy
    public void destroy1() {
        log.info("destroy1");
    }

    @Override
    public void destroy() throws Exception {
        log.info("destroy2");
    }

    public void destroy3() {
        log.info("destroy3");
    }
}
