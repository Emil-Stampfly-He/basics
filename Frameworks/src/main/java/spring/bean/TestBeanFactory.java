package spring.bean;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
public class TestBeanFactory {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 1. Bean的定义（BeanDefinition）
        // class, scope（单例/多例）, 初始化, 销毁
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Config.class)
                .setScope("singleton")
                .getBeanDefinition();
        beanFactory.registerBeanDefinition("config", beanDefinition);

        // 2. 给beanFactory添加BeanFactory后处理器与一些Bean后处理器
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);

        // 3. 让beanFactory执行BeanFactory后处理器
        beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values()
                .forEach(beanFactoryPostProcessor -> {
                    System.out.println(beanFactoryPostProcessor.getClass().getName());
                    beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
                });

        System.out.println();

        // 4. Bean后处理器，针对Bean的生命周期的各个阶段提供扩展，例如@Autowired
        beanFactory.getBeansOfType(BeanPostProcessor.class).values()
                .stream()
                .sorted(beanFactory.getDependencyComparator())
                .forEach(beanPostProcessor -> {
                    System.out.println(beanPostProcessor.getClass().getName());
                    beanFactory.addBeanPostProcessor(beanPostProcessor);
                });

        System.out.println();

        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }

        System.out.println();

        // 5. 提前创建好单例Bean
        beanFactory.preInstantiateSingletons();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
        System.out.println(beanFactory.getBean(Bean1.class).getBean2());

        System.out.println();
        System.out.println(beanFactory.getBean(Bean1.class).getInter());
    }

    @Configuration
    static class Config {
        @Bean
        public Bean1 bean1() {return new Bean1();}
        @Bean
        public Bean2 bean2() {return new Bean2();}
        @Bean
        public Bean3 bean3() {return new Bean3();}
        @Bean
        public Bean4 bean4() {return new Bean4();}
    }

    interface Inter { }

    static class Bean3 implements Inter { }
    static class Bean4 implements Inter { }

    @Slf4j
    static class Bean1 {
        @Autowired private Bean2 bean2;
        @Autowired @Resource(name = "bean4") private Inter bean3;

        public Bean1() {log.info("构造Bean1");}
        public Bean2 getBean2() {return bean2;}
        public Inter getInter() {return bean3;}
    }

    @Slf4j
    static class Bean2 {
        public Bean2() {log.info("构造Bean2");}
    }
}
