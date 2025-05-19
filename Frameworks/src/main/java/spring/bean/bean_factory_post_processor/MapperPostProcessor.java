package spring.bean.bean_factory_post_processor;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;

public class MapperPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanFactory) throws BeansException {
        try {
            PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
            // 潜在改进：不应该写死
            Resource[] resources = resourceResolver.getResources("classpath:spring/bean/bean_factory_post_processor/**/*.class");
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
            AnnotationBeanNameGenerator generator = new AnnotationBeanNameGenerator();

            for (Resource resource : resources) {
                MetadataReader reader = factory.getMetadataReader(resource);
                ClassMetadata classMetadata = reader.getClassMetadata();
                boolean isInterface = classMetadata.isInterface();

                if (isInterface) {
                    AbstractBeanDefinition beanDefinition1 = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class)
                            .addConstructorArgValue(classMetadata.getClassName())
                            .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                            .getBeanDefinition();
                    AbstractBeanDefinition beanDefinition2 = BeanDefinitionBuilder.genericBeanDefinition(classMetadata.getClassName())
                            .getBeanDefinition();

                    String beanName = generator.generateBeanName(beanDefinition2, beanFactory);
                    beanFactory.registerBeanDefinition(beanName, beanDefinition1);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        BeanDefinitionRegistryPostProcessor.super.postProcessBeanFactory(configurableListableBeanFactory);
    }
}
