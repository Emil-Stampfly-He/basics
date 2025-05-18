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
            // 潜在改进：将Config.class写死了，实际上应该检查所有类上是否有@ComponentScan
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
我们再来看一下配置类`Config`：
```java
@Configuration
@ComponentScan(basePackages = "spring.bean.bean_factory_post_processor")
public class Config {
    @Bean public Bean1 bean1() {return new Bean1();}

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
配置类所充当的角色本质上是一个工厂，而配置类中被加了`@Bean`注解的方法是工厂方法。之后要创建一个个对象，那么就直接调用工厂方法创建就好了。

为了拿到所有的加了`@Bean`的方法（“工厂方法”），我们还是使用`MetadataReader`。它不使用类加载机制，也不适用反射来获取类信息，性能较高。由于示例中只有`Config`这一个配置类，所以`getMetadataReader`方法中的参数写死了，实际上应该遍历所有扫描到的包：
```java
CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
// 资源路径写死，实际上应该遍历所有扫描到的包
MetadataReader reader = factory.getMetadataReader(new ClassPathResource("spring/bean/bean_factory_post_processor/Config.class"));
Set<MethodMetadata> annotatedMethods = reader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());
annotatedMethods.forEach(System.out::println);
```
可以看到`@Bean`方法都被找到了：
```aiignore
spring.bean.bean_factory_post_processor.Config.bean1()
spring.bean.bean_factory_post_processor.Config.sqlSessionFactoryBean(javax.sql.DataSource)
spring.bean.bean_factory_post_processor.Config.druidDataSource()
```
接下来我们就可以根据方法信息创建`BeanDefinition`：
```java
CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
MetadataReader reader = factory.getMetadataReader(new ClassPathResource("spring/bean/bean_factory_post_processor/Config.class"));
Set<MethodMetadata> annotatedMethods = reader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());

for (MethodMetadata annotatedMethod : annotatedMethods) {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition()
            .setFactoryMethodOnBean(annotatedMethod.getMethodName(), "config") // 工厂方法与工厂
            .getBeanDefinition();
    context.registerBeanDefinition(annotatedMethod.getMethodName(), beanDefinition); // Bean的名字就是方法的名字
}
```
在`.setFactoryMethodOnBean(annotatedMethod.getMethodName(), "config")`一步中，我们相当于指定了`@Bean`方法为工厂方法，`Config`类为工厂。拿到`beanDefinition`后就向容器中注册，通常来说Bean的名字就是方法的名字。

但到此还没有结束，启动就会发现有一个`@Bean`方法并没有被解析成功：`sqlSessionFactoryBean`。这个方法的不同之处在于它多了一个参数，这个参数需要我们手动指定自动装配策略，Spring才能解析参数，否则将导致参数解析模糊（没办法确定用哪个Bean）而出错：
```java
AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition()
        .setFactoryMethodOnBean(annotatedMethod.getMethodName(), "config") // 工厂方法与工厂
        .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
        .getBeanDefinition();
```
我们指定了**按构造函数参数自动注入**，Spring会尝试匹配构造函数参数的类型和名称。为什么要使用“按构造函数参数自动注入”？前面提到，`@Bean`方法本质上是一个工厂方法，它的参数是构造函数参数的形式：
```java
@Bean public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) {/*...*/}
```
`AUTOWIRE_CONSTRUCTOR` 会告诉 Spring 去匹配工厂方法的参数并自动装配（就像调用构造函数一样），这是唯一能保证工厂方法参数被正确解析的自动装配策略。

另外的三个自动装配策略是（还有一个过时了的策略，我们忽略不计）：
1. `AUTOWIRE_NO`：默认选项，不进行自动装配。这是最开始导致错误的原因，Spring不知道要怎么寻找Bean注入。
2. `AUTOWIRE_BY_NAME`：按属性名称自动注入，即根据属性名匹配容器中同名的Bean。适合`setter`注入，不符合我们的场景。
3. `AUTOWIRE_BY_TYPE`：按属性类型自动注入，即根据属性类型匹配容器中唯一的Bean。适合字段注入，不符合我们的场景。

指定了装配策略就能运行成功了：
```aiignore
17:48:42.359 [main] INFO spring.bean.bean_factory_post_processor.Bean1 -- Managed by Spring
config
bean1
sqlSessionFactoryBean
druidDataSource
17:48:42.489 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-0} closing ...
```
那么到此我们就成功解析了`@Bean`注解。但是`@Bean`注解里面是有很多属性的，我们完全忽略掉了，比如：
```java
@Bean(initMethod = "init") public DruidDataSource druidDataSource() {/*...*/}
```
解决办法很简单，我们只需要拿到这个属性，并给`beanDefinition`设置这个属性的值即可：
```java
CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
// MetadataReader不走类加载，效率也比反射高
MetadataReader reader = factory.getMetadataReader(new ClassPathResource("spring/bean/bean_factory_post_processor/Config.class"));
Set<MethodMetadata> annotatedMethods = reader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());

    for (MethodMetadata annotatedMethod : annotatedMethods) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();

    // 拿到@Bean中的属性
    Map<String, Object> annotationAttributes = annotatedMethod.getAnnotationAttributes(Bean.class.getName());
    // 潜在的改进之处：应当检查@Bean的所有属性并设置给builder
    String initMethod = annotationAttributes.get("initMethod").toString();
    // 如果initMethod属性不为空，则向builder中设置属性
    if (!initMethod.isEmpty()) {
        builder.setInitMethodName(initMethod);
    }

    AbstractBeanDefinition beanDefinition = builder.setFactoryMethodOnBean(annotatedMethod.getMethodName(), "config")
            .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
            .getBeanDefinition();
    context.registerBeanDefinition(annotatedMethod.getMethodName(), beanDefinition);
}
```
当然，`@Bean`有非常多的属性，理应将所有属性都加入`beanDefinition`中，但这里为了方便演示，我们只加入了`initMethod`属性。

现在`DruidDataSource`会打印初始化的日志了：
```aiignore
18:14:31.755 [main] INFO spring.bean.bean_factory_post_processor.Bean1 -- Managed by Spring
18:14:31.876 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} inited
config
bean1
sqlSessionFactoryBean
druidDataSource
18:14:31.999 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closing ...
18:14:32.000 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closed
```
到此，`@Bean`的解析也完成了，可以将上述逻辑提取到`AnnotationBeanPostProcessor`中，作为一个Bean工厂后处理器。与`ComponentScanPostProcessor`一样，需要进行一些小改动：
```java
public class AnnotationBeanPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        // 改动1：try-catch捕获异常
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
                // 改动2
                if (configurableListableBeanFactory instanceof DefaultListableBeanFactory beanFactory) {
                    beanFactory.registerBeanDefinition(annotatedMethod.getMethodName(), beanDefinition);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```
抽取后，我们就可以直接在容器中注入这个Bean工厂后处理器了：
```java
public class TestBeanFactoryPostProcessor {
    public static void main(String[] args) throws IOException {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("config", Config.class);
        context.registerBean(AnnotationBeanPostProcessor.class);
        
        context.refresh();
        for (String beanDefinitionName : context.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        context.close();
    }
}
```
可以看到效果完全是相同的：
```aiignore
18:23:59.202 [main] INFO spring.bean.bean_factory_post_processor.Bean1 -- Managed by Spring
18:23:59.356 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} inited
config
spring.bean.bean_factory_post_processor.AnnotationBeanPostProcessor
bean1
sqlSessionFactoryBean
druidDataSource
18:23:59.484 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closing ...
18:23:59.484 [main] INFO com.alibaba.druid.pool.DruidDataSource -- {dataSource-1} closed
```

## 3. MyBatis中`@Mapper`的处理
Mapper层一般都是一些接口，而Spring是不能直接管理接口的。要让Spring将接口转换为`BeanDefinition`从而放入容器中进行管理，需要一些其他的手段。

### 3.1. 接口转为对象
我们可以创建一个工厂（这个工厂本身是一个Bean），让这个工厂为我们代理mapper。由于所有的mapper都需要依赖同一个`SqlSessionFactory`才能执行SQL、管理事务、缓存等，所以需要将`SqlSessionFactory`注入：
```java
// Config.java
@Bean
public MapperFactoryBean<Mapper1> mapper1(SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<Mapper1> factoryBean = new MapperFactoryBean<>(Mapper1.class);
    factoryBean.setSqlSessionFactory(sqlSessionFactory);
    return factoryBean;
}

@Bean
public MapperFactoryBean<Mapper2> mapper2(SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<Mapper2> factoryBean = new MapperFactoryBean<>(Mapper2.class);
    factoryBean.setSqlSessionFactory(sqlSessionFactory);
    return factoryBean;
}
```
但这样添加有一个缺点：如果我们的mapper接口非常多，那么我们需要写大量重复的代码。能不能一次性将所有mapper全部扫描到并批量进行添加呢？

### 3.2. 手动模拟解析`@MapperScan`与`@Mapper`
