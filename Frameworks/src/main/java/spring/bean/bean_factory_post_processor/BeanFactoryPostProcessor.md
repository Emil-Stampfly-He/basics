# Bean工厂后处理器
Bean工厂后处理器的功能与Bean后处理器的功能类似，主要是为Bean工厂补充一些功能并解析一些Bean。

## 1. 两个常见的Bean工厂处理器

### 1.1. 解析`@Component`、`@Configuration`和`@Bean`
假设我们有如下`Config`类：
```java
@Configuration
@ComponentScan("spring.bean.bean_factory_post_processor")
public class Config {
    @Bean
    public Bean1 bean1() {return new Bean1();}

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean;
    }

    @Bean(initMethod = "init")
    public DruidDataSource druidDataSource() {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUrl("jdbc:mysql://localhost:3306/test");
        druidDataSource.setUsername("root");
        druidDataSource.setPassword("root");
        return druidDataSource;
    }
}
```
其中`Bean1`类如下：
```java
@Slf4j public class Bean1 { public Bean1() {log.info("Managed by Spring");}}
```
`@ComponentScan`是让Spring能够扫描到这个配置类。这个配置类配置了3个Bean，很显然，如果我们现在有一个干净的容器`GenericApplicationContext`，那么配置类中的Bean是不会被解析出来的，因为缺少了解析`@Configuration`和`@ComponentScan`的Bean工厂后处理器：
```java
public class BeanFactoryPostProcessor {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("config", Config.class);
        context.refresh();
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        context.close();
    }
}
```
为了让`Config`中的`@Bean`被全部解析，我们加入`ConfigurationClassPostProcessor`来处理`@Configuration`和`@ComponentScan`注解：
```java
context.registerBean(ConfigurationClassPostProcessor.class);
```
再次打印，我们就能够看到容器中有非常多的Bean了：
```aiignore
13:18:16.323 [main] INFO spring.bean.bean_factory_post_processor.Bean1 -- Managed by Spring
13:18:16.412 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} inited
config
org.springframework.context.annotation.ConfigurationClassPostProcessor
bean1
sqlSessionFactoryBean
druidDataSource
13:18:16.486 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closing ...
13:18:16.486 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closed
```
实际上，这个Bean后处理器不止能够解析`@ComponentScan`、`@Configuration`和`@Bean`，还能解析`@Component`、`@Import`和`@ImportResource`。我们再拿`@Component`注解举例，因为它非常常用。新建立一个加了`@Component`注解的`Bean2`：
```java
@Slf4j @Component public class Bean2 { public Bean2() { log.info("Managed by Spring");}}
```
再次启动`main`方法，我们就能看到`Bean2`也作为一个Bean被Spring添加到容器中。

### 1.2. 解析`@Mapper`
当我们使用MyBatis的`@Mapper`注解标记一个mapper类时，这个类会识别到带有`@Mapper`注解的类，并将它们的BeanDefinition`也补充到Bean工厂中。
```java
@Mapper public interface Mapper1 { }
@Mapper public interface Mapper2 { }
```
现在，我们往容器中加入这个Bean工厂后处理器（注意这里还要传入包的名称，否则该后处理器无法知道扫描哪个包）：
```java
context.registerBean(MapperScannerConfigurer.class,
        bd -> bd.getPropertyValues().add("basePackage", "spring.bean.bean_factory_post_processor.mapper"));
```
这样Spring容器也管理了两个mapper：
```aiignore
13:32:39.673 [main] INFO spring.bean.bean_factory_post_processor.component.Bean2 -- Managed by Spring
13:32:39.681 [main] INFO spring.bean.bean_factory_post_processor.Bean1 -- Managed by Spring
13:32:39.768 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} inited
config
org.springframework.context.annotation.ConfigurationClassPostProcessor
org.mybatis.spring.mapper.MapperScannerConfigurer
bean2
bean1
sqlSessionFactoryBean
druidDataSource
mapper1
mapper2
org.springframework.context.annotation.internalConfigurationAnnotationProcessor
org.springframework.context.annotation.internalAutowiredAnnotationProcessor
org.springframework.context.annotation.internalCommonAnnotationProcessor
org.springframework.context.event.internalEventListenerProcessor
org.springframework.context.event.internalEventListenerFactory
13:32:39.894 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closing ...
13:32:39.896 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closed
```
我们注意到，输出中又多出来了5个之前已经见到过的Bean后处理器，这是`MapperScannerConfigurer`自动为我们加上的。

## 2. 手动模拟解析注解

### 2.1. `@Component`及其派生注解
上面两个Bean工厂后处理器是怎么找到注解、解析注解并注入的呢？我们接下来就来模拟一下它们的功能。

首先我们要找到注解`@ComponentScan`并把要扫描的所有包拿到，这样才能知道后续要从哪些包中扫描其他注解：
```java
Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
if (componentScan.isPresent()) {
    for (String basePackage :componentScan.get().basePackages()){
        System.out.println(basePackage);
    }
}
```
程序找到了以下包：
```aiignore
spring.bean.bean_factory_post_processor
```
我们需要把这个包名转变成这种格式的路径：`spring/bean/bean_factory_post_processor/**/*.class`，根据这个路径我们才能获取资源：
```java
Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
    if (componentScan.isPresent()) {
    for (String basePackage :componentScan.get().basePackages()){
        String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";
        Resource[] resources = context.getResources(path);
    }
}
```
`getResource`方法是最开始所讲到的`ApplicationContext`对`BeanFactory`的4项功能扩展之一`ResourcePatternResolver`的方法。

那么接下来我们就可以针对获取到的资源进行分析，看看哪些类上加了`@Component`注解：
```java
Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
if (componentScan.isPresent()) {
    for (String basePackage :componentScan.get().basePackages()){
        String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";
        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory(); // 资源读取器的工厂
        Resource[] resources = context.getResources(path);
        
        for (Resource resource : resources) {
            MetadataReader reader = factory.getMetadataReader(resource); // 资源读取器
            
            System.out.println("类名：" + reader.getClassMetadata().getClassName());
            System.out.println("是否加了@Component：" + reader.getAnnotationMetadata().hasAnnotation(Component.class.getName()));
        }
    }
}
```
只有`Bean2`加了`@Component`，输出应当只有`Bean2`为`true`：
```aiignore
类名：spring.bean.bean_factory_post_processor.Bean1
是否加了@Component：false
类名：spring.bean.bean_factory_post_processor.TestBeanFactoryPostProcessor
是否加了@Component：false
类名：spring.bean.bean_factory_post_processor.component.Bean2
是否加了@Component：true <---------
类名：spring.bean.bean_factory_post_processor.Config
是否加了@Component：false
类名：spring.bean.bean_factory_post_processor.mapper.Mapper1
是否加了@Component：false
类名：spring.bean.bean_factory_post_processor.mapper.Mapper2
是否加了@Component：false
```
但是有些注解是`@Component`的派生注解，例如`@Controller`、`@Service`等，我们仅仅判断类上是否有`@Component`注解是不够的，因为程序不会自动识别其他`@Component`的派生注解，比如`Config`类上虽然加了`@Configuration`，而且`@Configuration`中包含了`@Component`，但是输出仍然为`false`。我们需要使用`hasMetaAnnotation`方法来查找`@Component`作为元注解的情况：
```java
Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
    if (componentScan.isPresent()) {
    for (String basePackage :componentScan.get().basePackages()){
        String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";
        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory(); // 资源读取器的工厂
        Resource[] resources = context.getResources(path);
        
        for (Resource resource : resources) {
            MetadataReader reader = factory.getMetadataReader(resource); // 资源读取器

            String className = reader.getClassMetadata().getClassName();
            boolean hasComponent = reader.getAnnotationMetadata().hasAnnotation(Component.class.getName());
            boolean hasComponentDerivative = reader.getAnnotationMetadata().hasMetaAnnotation(Component.class.getName());
            
            System.out.println("类名：" + className);
            System.out.println("是否加了@Component：" + hasComponent);
            System.out.println("是否加了@Component派生注解：" + hasComponentDerivative);
        }
    }
}
```
加好之后，现在假设我们现在有一个`Controller`是`Bean3`：
```java
@Controller public class Bean3 { }
```
那么程序会判断`Bean3`虽然没有加`@Component`注解，但是加了`@Component`的派生注解。包括`Config`类也是一样，`@Configuration`本身包含`@Component`：
```aiignore
类名：spring.bean.bean_factory_post_processor.Bean1
是否加了@Component：false
是否加了@Component派生注解：false
类名：spring.bean.bean_factory_post_processor.TestBeanFactoryPostProcessor
是否加了@Component：false
是否加了@Component派生注解：false
类名：spring.bean.bean_factory_post_processor.component.Bean2
是否加了@Component：true
是否加了@Component派生注解：false
类名：spring.bean.bean_factory_post_processor.component.Bean3
是否加了@Component：false
是否加了@Component派生注解：true <----------
类名：spring.bean.bean_factory_post_processor.Config
是否加了@Component：false
是否加了@Component派生注解：true <----------
类名：spring.bean.bean_factory_post_processor.mapper.Mapper1
是否加了@Component：false
是否加了@Component派生注解：false
类名：spring.bean.bean_factory_post_processor.mapper.Mapper2
是否加了@Component：false
是否加了@Component派生注解：false
```
既然我们知道了所有加了`@Component`的类，那么现在要做的就是将它们全部加入容器中：
1. 根据类名创建`beanDefinition`
2. 创建`beanName`，可以使用工具类`AnnotationBeanNameGenerator`
3. 根据`beanName`和`beanDefinition`创建Bean

```java
Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
    if (componentScan.isPresent()) {
    for (String basePackage :componentScan.get().basePackages()){
        String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

        CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory(); // 资源读取器的工厂
        Resource[] resources = context.getResources(path);
        AnnotationBeanNameGenerator generator = new AnnotationBeanNameGenerator(); // 创建Bean名称的工具类

        for (Resource resource : resources) {
            MetadataReader reader = factory.getMetadataReader(resource); // 资源读取器

            String className = reader.getClassMetadata().getClassName();
            boolean hasComponent = reader.getAnnotationMetadata().hasAnnotation(Component.class.getName());
            boolean hasComponentDerivative = reader.getAnnotationMetadata().hasMetaAnnotation(Component.class.getName());

            if (hasComponent || hasComponentDerivative) {
                AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(className)
                        .getBeanDefinition();
                DefaultListableBeanFactory beanFactory = context.getDefaultListableBeanFactory();
                String beanName = generator.generateBeanName(beanDefinition, beanFactory);
                beanFactory.registerBeanDefinition(beanName, beanDefinition);
                
//                或者直接使用context注册beanDefinition，因为GenericApplicationCOntext实现了BeanDefinitionRegistry接口
//                String beanName = generator.generateBeanName(beanDefinition, context);
//                context.registerBeanDefinition(beanName, beanDefinition);
            }
        }
    }
}
```
再次运行容器，我们可以看到：
```java
config
bean2
bean3
```
确实已经解析成功，尽管`Config`类下的`@Bean`注解还没有被处理。这样我们就完成了针对`@Component`及其派生注解的Bean工厂后处理器的功能，可以将上述逻辑提取到一个新类中，当然会需要微调：
* 原来我们直接使用`context.getResources(path)`来获得资源，但现在容器变量不存在，所以我们使用另一个资源解析器`PathMatchingResourcePatternResolver`：
```java
PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
Resource[] resources = resourceResolver.getResources(path);
```
* 由于我们不能直接使用`DefaultListableBeanFactory`了，而`ConfigurableListableBeanFactory`没有实现`BeanDefinitionRegistry`的接口，意味着它不能注册`BeanDefinition`，所以这里我们还要判断一下`beanFactory`的类型：
```java
if (configurableListableBeanFactory instanceof DefaultListableBeanFactory beanFactory) {
    String beanName = generator.generateBeanName(beanDefinition, beanFactory);
    beanFactory.registerBeanDefinition(beanName, beanDefinition);
}
```
* 异常使用`try-catch`块处理
```java
public class ComponentScanPostProcessor implements BeanFactoryPostProcessor {
    @Override // context.refresh()时这个方法会被调用
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        // 改动3
        try {
            // 不足之处：将Config.class写死了，实际上应该检查所有类上是否有@ComponentScan
            Optional<ComponentScan> componentScan = AnnotationUtils.findAnnotation(Config.class, ComponentScan.class);
            if (componentScan.isPresent()) {
                for (String basePackage : componentScan.get().basePackages()) {
                    String path = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

                    CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
                    // 改动1：资源解析器
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

                            // 改动2：判断类型，确保beanFactory拥有BeanDefinitionRegistry的beanDefinition注册方法
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
```
这个方法会在`context.refresh()`中被回调。我们这样验证，将我们自定义的`ComponentScanPostProcessor`Bean工厂后处理器加入到容器`context`中：
```java
public class TestBeanFactoryPostProcessor {
    public static void main(String[] args) throws IOException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("config", Config.class);
        context.registerBean(ComponentScanPostProcessor.class);
        
        context.refresh();
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        context.close();
    }
}
```

### 2.2. `@Bean`



## 3. Mapper