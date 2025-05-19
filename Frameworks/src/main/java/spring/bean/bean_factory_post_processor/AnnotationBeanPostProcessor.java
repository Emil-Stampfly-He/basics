package spring.bean.bean_factory_post_processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

// TODO：改成BeanDefinitionRegistryPostProcessor
public class AnnotationBeanPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        try {
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();

            // 潜在改进：应当遍历所有扫描到的包
            MetadataReader reader = factory.getMetadataReader(new ClassPathResource("spring/bean/bean_factory_post_processor/Config.class"));
            Set<MethodMetadata> annotatedMethods = reader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());

            for (MethodMetadata annotatedMethod : annotatedMethods) {
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();

                // 潜在改进：应当检查@Bean的所有属性
                Map<String, Object> annotationAttributes = annotatedMethod.getAnnotationAttributes(Bean.class.getName());
                String initMethod = annotationAttributes.get("initMethod").toString();
                if (!initMethod.isEmpty()) {
                    builder.setInitMethodName(initMethod);
                }

                // 潜在改进：应当遍历所有@Configuration类，而不是写死config
                AbstractBeanDefinition beanDefinition = builder.setFactoryMethodOnBean(annotatedMethod.getMethodName(), "config")
                        .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
                        .getBeanDefinition();
                if (configurableListableBeanFactory instanceof DefaultListableBeanFactory beanFactory) {
                    beanFactory.registerBeanDefinition(annotatedMethod.getMethodName(), beanDefinition);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
