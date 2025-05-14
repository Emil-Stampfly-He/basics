package spring.bean.bean_post_processor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public class Bean1 {
    private static final Logger log = LoggerFactory.getLogger(Bean1.class);
    private Bean2 bean2;
    @Autowired private Bean3 bean3;
    private String home;

    @Autowired
    public void setBean2(Bean2 bean2) {
        log.info("@Autowired生效：{}", bean2);
        this.bean2 = bean2;
    }

    @Resource
    public void setBean3(Bean3 bean3) {
        log.info("@Resource生效：{}", bean3);
        this.bean3 = bean3;
    }

    @Autowired
    public void setHome(@Value("${JAVA_HOME}") String home) {
        log.info("@Value生效：{}", home);
        this.home = home;
    }

    @PostConstruct
    public void init() {
        log.info("@PostConstruct生效");
    }

    @PreDestroy
    public void destroy() {
        log.info("@PreDestroy生效");
    }

    @Override
    public String toString() {
        return "Bean1 [home=" + home + ", bean2=" + bean2 + "], bean3=" + bean3 + "]";
    }
}
