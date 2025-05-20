package spring.bean.aware;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MyConfig implements InitializingBean, ApplicationContextAware {
//    @Autowired
//    public void setApplicationContext(ApplicationContext applicationContext) {
//        log.info("Inject ApplicationContext");
//    }
//
//    @PostConstruct
//    public void init() {
//        log.info("Initialization");
//    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Bean {} was initialized", this);
    }

    @Bean
    public BeanFactoryPostProcessor processor() {
        return _ -> log.info("Processor executed");
    }
}
