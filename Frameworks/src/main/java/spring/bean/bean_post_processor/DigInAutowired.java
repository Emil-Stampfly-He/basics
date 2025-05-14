package spring.bean.bean_post_processor;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class DigInAutowired {

    public static void main(String[] args) throws Throwable {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("bean2", new Bean2()); // 直接注入了一个成品Bean，跳过了创建过程、依赖注入和初始化
        beanFactory.registerSingleton("bean3", new Bean3());
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver()); // @Value

        // 1. 查找哪些字段、方法加了@Autowired
        AutowiredAnnotationBeanPostProcessor beanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
        beanPostProcessor.setBeanFactory(beanFactory);

        Bean1 bean1 = new Bean1();
//        System.out.println(bean1); // bean1字段全是null
//
//        beanPostProcessor.postProcessProperties(null, bean1, "bean1"); // 执行依赖注入@Autowired @Value
//
//        System.out.println(bean1); // bean1字段有值
        Method findAutowiringMetadata = AutowiredAnnotationBeanPostProcessor.class.getDeclaredMethod("findAutowiringMetadata", String.class, Class.class, PropertyValues.class);
        findAutowiringMetadata.setAccessible(true);
        InjectionMetadata metadata = (InjectionMetadata) findAutowiringMetadata.invoke(beanPostProcessor, "bean1", Bean1.class, null);// 获取Bean1上加了@Autowired和@Value的成员变量和方法参数信息

        // 2. 调用InjectionMetadata来进行依赖注入，注入时按照类型查找值
//        metadata.inject(bean1, "bean1", null);
//        System.out.println(bean1);

        // 3. 如何按类型查找值（inject内部细节）
        // 3.1. bean3
        Field bean3 = Bean1.class.getDeclaredField("bean3");
        DependencyDescriptor desc1 = new DependencyDescriptor(bean3, false);
        Object o = beanFactory.doResolveDependency(desc1, "bean3", null, null);
        System.out.println(o);

        bean3.setAccessible(true);
        bean3.set(bean1, o);
        System.out.println(bean1);

        // 3.2. setBean2
        Method setBean2 = Bean1.class.getDeclaredMethod("setBean2", Bean2.class);
        DependencyDescriptor desc2 = new DependencyDescriptor(new MethodParameter(setBean2, 0), true);
        Object o1 = beanFactory.doResolveDependency(desc2, "bean2", null, null);
        System.out.println(o1);

        setBean2.setAccessible(true);
        setBean2.invoke(bean1, beanFactory.getBean(Bean2.class));
        System.out.println(bean1);

    }
}
