package spring.bean.aware;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

@Slf4j
public class AwareAndInitializingBean {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        // context.registerBean("myBean", MyBean.class);
        context.registerBean("myConfig", MyConfig.class);
        context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
        context.registerBean(CommonAnnotationBeanPostProcessor.class);
        context.registerBean(ConfigurationClassPostProcessor.class);
        context.refresh();
        context.close();
    }
}
