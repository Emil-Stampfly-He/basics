package spring.bean.bean_factory_post_processor;

import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;

public class TestBeanFactoryPostProcessor {
    public static void main(String[] args) throws IOException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("config", Config.class);

//        // BeanFactory后处理器
//        // @ComponentScan, @Bean, @Import, @ImportResource
//        context.registerBean(ConfigurationClassPostProcessor.class);
//        // @MapperScanner，@Mapper
//        // 还会加上常见的Bean后处理器
//        context.registerBean(MapperScannerConfigurer.class,
//                bd -> bd.getPropertyValues().add("basePackage", "spring.bean.bean_factory_post_processor.mapper"));

        context.registerBean(ComponentScanPostProcessor.class);

        context.refresh();

        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }

        context.close();
    }
}
