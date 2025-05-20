# `Aware`与`InitializingBean`

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

## 1. `Aware`和`InitializingBean`概览
`Aware`和`InitializingBean`是一些注解功能的内置实现，换句话说，就是不依靠注解而实现注解的功能。

### 1.1. `Aware`
`Aware`接口用于注入一些与容器相关信息，例如：
* `BeanNameAware`注入Bean的名字
* `BeanFactoryAware`注入Bean工厂容器
* `ApplicationContextAware`注入`ApplicationContext`容器
* `EmbeddedValueResolverAware`用于解析`${}`通配符（在`@Value`注解中的功能）

我们演示一下`BeanNameAware`这个接口的作用。假设我们有一个`Bean`实现了这个接口，那么它需要重写`setBeanName`方法：
```java
@Slf4j
public class MyBean implements BeanNameAware {
    @Override
    public void setBeanName(String name) {log.info("Bean {} name is {}", this, name);}
}
```
很显然这个方法未来被Spring调用后，`name`会被注入Bean的名字。我们创建一个容器并注册`MyBean`：
```java
@Slf4j
public class AwareAndInitializingBean {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("myBean", MyBean.class);
        context.refresh();
        context.close();
    }
}
```
输出为：
```aiignore
19:03:22.285 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@71bbf57e name is myBean
```
说明`setBeanName`方法被调用，而且`name`被注入了`myBean`的值。

再以`ApplicationContextAware`注入`ApplicationContext`容器为例，我们让`MyBean`实现这个接口：
```java
@Slf4j
public class MyBean implements BeanNameAware, ApplicationContextAware {
    @Override
    public void setBeanName(String name) {log.info("Bean {} name is {}", this, name);}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }
}
```
我们会发现`GenericApplicationContext`也被成功注入进去了：
```aiignore
19:11:35.672 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@71bbf57e name is myBean
19:11:35.684 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@71bbf57e applicationContext is org.springframework.context.support.GenericApplicationContext@7bb11784, started on Tue May 20 19:11:35 BST 2025
```

### 1.2. `InitializingBean`
`InitializingBean`接口顾名思义是用于初始化Bean的。如果我们让一个Bean实现了这个接口并重写了`afterPropertiesSet`方法，那么这个Bean就会按照这个方法中的内容进行初始化：
```java
@Slf4j
public class MyBean implements BeanNameAware, ApplicationContextAware,
        InitializingBean {
    @Override
    public void setBeanName(String name) {log.info("Bean {} name is {}", this, name);}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {log.info("Bean {} was initialized", this);}
}
```
输出如下：
```aiignore
19:14:24.101 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@7f13d6e name is myBean
19:14:24.107 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@7f13d6e applicationContext is org.springframework.context.support.GenericApplicationContext@7bb11784, started on Tue May 20 19:14:24 BST 2025
19:14:24.107 [main] INFO spring.bean.aware.MyBean -- Bean spring.bean.aware.MyBean@7f13d6e was initialized
```
可以看到`MyBean`按照`afterPropertiesSet`的方法进行了初始化。

## 2. 注解失效分析
看到这里，我们可能会想，除了第一个`setBeanName`好像不能用注解，另外两个方法完全可以用`@Autowired`和`@PostConstruct`代替，为什么还要去大费周章地实现接口呢？

这里我们需要明确一下注解和`Aware`系列接口还有`InitializingBean`接口的区别：`@Autowired`等注解的解析需要用到Bean后处理器，属于扩展功能；而`Aware`系列接口和`InitializingBean`接口属于内置功能，不需要任何扩展（后处理器）Spring就能识别并实现。在某些情况下，扩展功能可能失效。这个时候我们就不得不让这个Bean实现`Aware`或`InitializingBean`接口来实现相同的功能。

我们现在给`MyBean`加上两个分别用`@Autowired`和`@PostConstruct`的注解：
```java
@Slf4j
public class MyBean implements BeanNameAware, ApplicationContextAware,
        InitializingBean {
    @Override
    public void setBeanName(String name) {log.info("Bean {} name is {}", this, name);}

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {log.info("Bean {} was initialized", this);}

    @Autowired
    public void autowired(ApplicationContext context) {log.info("Bean {} autowired using @Autowired", this);}

    @PostConstruct
    public void init() {log.info("Bean {} postConstruct using @PostConstruct", this);}
}
```
再次运行，我们会发现后面两个加了注解的方法根本没有生效，只有使用`Aware`和`InitializingBean`的方法才生效了。这就是前面所说的“某些情况”的一种。要想让注解被解析也很简单，我们往容器中添加这些注解的后处理器就好了：
```java
@Slf4j
public class AwareAndInitializingBean {
    public static void main(String[] args) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("myBean", MyBean.class);
        context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
        context.registerBean(CommonAnnotationBeanPostProcessor.class);
        context.refresh();
        context.close();
    }
}
```
但还有一种情况是：解释加了后处理器，注解也仍然不会被解析。我们现在分析一下这种更特殊的情况。假设我们现在有这样一个配置类。
这个配置类除了进行依赖注入和初始化，我们还配置了一个Bean：
```java
@Slf4j
@Configuration
public class MyConfig {
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        log.info("Inject ApplicationContext");
    }

    @PostConstruct
    public void init() {log.info("Initialization");}

    @Bean
    public BeanFactoryPostProcessor processor() {return _ -> log.info("Processor executed");}
}
```
为了让`@Configuration`被解析，我们要在容器中添加Bean工厂后处理器：
```java
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
```
运行这个`main`方法：
```aiignore
19:43:28.869 [main] INFO org.springframework.context.annotation.ConfigurationClassEnhancer -- @Bean method MyConfig.processor is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface. This will result in a failure to process annotations such as @Autowired, @Resource and @PostConstruct within the method's declaring @Configuration class. Add the 'static' modifier to this method to avoid these container lifecycle issues; see @Bean javadoc for complete details.
19:43:28.872 [main] INFO spring.bean.aware.MyConfig -- Processor executed
```
我们不仅看到了，在容器拥有齐全的Bean工厂后处理器和Bean后处理器的情况下，`@Autowired`和`@PostConstruct`注解也依然完全没有生效的情况，还能看到Spring给出的相应的警告。这是为什么？

`refresh`方法的执行顺序是这样的：
1. 添加Bean工厂后处理器拿到所有`BeanDefinition`
2. 添加Bean后处理器
3. 创建并初始化单例Bean
   1. 依赖注入扩展（比如`@Value`和`@Autowired`
   2. 初始化扩展（`@PostConstruct`）
   3. 执行`Aware`和`InitializingBean`
   4. 创建Bean成功

在我们的`MyConfig`类中，我们配置了一个`@Bean`，这个Bean是Bean工厂后处理器。按道理，如果`refresh`要进行第一步（拿到Bean工厂后处理器），就需要先把这个`@Bean`解析出来，后续才能去配置Bean工厂后处理器。那么对于`MyConfig`配置类来说，执行顺序就会被翻转：
1. 创建并初始化单例Bean：此时还没有Bean后处理器，注解无法被解析
   1. 执行`Aware`和`InitializingBean`
   2. 创建Bean成功
2. 添加Bean工厂后处理器拿到所有`BeanDefinition`
3. 添加Bean后处理器

由于Bean后处理器解析注解在后，而配置类的解析在前，所以注解就没有机会发挥作用。一个简单的解决方法就是使用内置解析接口`Aware`系列与`InitializingBean`：
```java
@Slf4j
@Configuration
public class MyConfig implements InitializingBean, ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("Bean {} applicationContext is {}", this, applicationContext);
    }

    @Override
    public void afterPropertiesSet() throws Exception {log.info("Bean {} was initialized", this);}

    @Bean
    public BeanFactoryPostProcessor processor() {return _ -> log.info("Processor executed");}
}
```
这样就能够实现原本我们期待`@Autowired`和`@PostConfiguration`同样的效果了：
```aiignore
19:58:29.404 [main] INFO spring.bean.aware.MyConfig -- Bean spring.bean.aware.MyConfig$$SpringCGLIB$$0@6eda5c9 applicationContext is org.springframework.context.support.GenericApplicationContext@7bb11784, started on Tue May 20 19:58:29 BST 2025
19:58:29.407 [main] INFO spring.bean.aware.MyConfig -- Bean spring.bean.aware.MyConfig$$SpringCGLIB$$0@6eda5c9 was initialized
19:58:29.409 [main] INFO org.springframework.context.annotation.ConfigurationClassEnhancer -- @Bean method MyConfig.processor is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface. This will result in a failure to process annotations such as @Autowired, @Resource and @PostConstruct within the method's declaring @Configuration class. Add the 'static' modifier to this method to avoid these container lifecycle issues; see @Bean javadoc for complete details.
19:58:29.410 [main] INFO spring.bean.aware.MyConfig -- Processor executed
```
内置的注入和初始化不受扩展功能的影响，总是会被执行。实际上Spring框架内部也经常使用它们。
