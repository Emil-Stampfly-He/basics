package spring.bean.bean_factory_post_processor;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

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

        // context.registerBean(ComponentScanPostProcessor.class);
        context.registerBean(AnnotationBeanPostProcessor.class);
        context.registerBean(MapperPostProcessor.class);

        context.refresh();

        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }

        context.close();
    }
}
