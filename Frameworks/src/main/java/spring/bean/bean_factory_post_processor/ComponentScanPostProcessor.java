package spring.bean.bean_factory_post_processor;

import org.junit.platform.commons.util.AnnotationUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

public class ComponentScanPostProcessor implements BeanFactoryPostProcessor {

    @Override // context.refresh()时这个方法会被调用
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        try {
            // 不足之处：将Config.class写死了，实际上应该检查所有类上是否有@ComponentScan
            Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
            if (componentScan.isPresent()) {
                for (String basePackage :componentScan.get().basePackages()){
                    String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

                    CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
                    PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
                    Resource[] resources = resourceResolver.getResources(path);
                    AnnotationBeanNameGenerator generator = new AnnotationBeanNameGenerator();

                    for (Resource resource : resources) {
                        MetadataReader reader = factory.getMetadataReader(resource);

                        String className = reader.getClassMetadata().getClassName();
                        boolean hasComponent = reader.getAnnotationMetadata().hasAnnotation(Component.class.getName());
                        boolean hasComponentDerivative = reader.getAnnotationMetadata().hasMetaAnnotation(Component.class.getName());

                        if (hasComponent || hasComponentDerivative) {
                            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(className).getBeanDefinition();

                            if (configurableListableBeanFactory instanceof DefaultListableBeanFactory beanFactory) {
                                String beanName = generator.generateBeanName(beanDefinition, beanFactory);
                                beanFactory.registerBeanDefinition(beanName, beanDefinition);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
