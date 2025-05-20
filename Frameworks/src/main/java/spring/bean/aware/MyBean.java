package spring.bean.aware;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
public class MyBean implements BeanNameAware, ApplicationContextAware,
        InitializingBean {
    @Override
    public void setBeanName(String name) {
        log.info("Bean {} name is {}", this, name);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Bean {} was initialized", this);
    }

    @Autowired
    public void autowired(ApplicationContext context) {
        log.info("Bean {} autowired using @Autowired", this);
    }

    @PostConstruct
    public void init() {
        log.info("Bean {} postConstruct using @PostConstruct", this);
    }
}
