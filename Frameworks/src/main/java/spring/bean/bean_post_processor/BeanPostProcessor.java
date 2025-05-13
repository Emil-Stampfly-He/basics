package spring.bean.bean_post_processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;

@Slf4j
public class BeanPostProcessor {
    public static void main(String[] args) {
        // GenericApplicationContext是一个“干净”的容器
        GenericApplicationContext context = new GenericApplicationContext();

        context.registerBean("bean1", Bean1.class);
        context.registerBean("bean2", Bean2.class);
        context.registerBean("bean3", Bean3.class);
        context.registerBean("bean4", Bean4.class);

        context.getDefaultListableBeanFactory().setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        // 解析@Autowired和@Value
        context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
        // 解析@Resource，@PostConstruct和@PreDestory
        context.registerBean(CommonAnnotationBeanPostProcessor.class);
        // 解析@ConfigurationProperties
        ConfigurationPropertiesBindingPostProcessor.register(context.getDefaultListableBeanFactory());
        System.out.println("系统java.home = " + System.getProperty("java.home"));
        System.out.println("系统java.version = " + System.getProperty("java.version"));

        context.refresh();

        System.out.println(context.getBean(Bean4.class));

        context.close();
    }
}
