# Bean后处理器

>**本笔记基于黑马程序员 Spring高级源码解读**
>
> 更美观清晰的版本在：[**Github**](https://github.com/Emil-Stampfly-He/basics)

## 0. 前置知识：Bean的生命周期
简单来说，Bean的生命周期大致为：
```aiignore
注册 → 依赖注入 → 初始化 → 销毁
```
后处理器的作用时机一般会在依赖注入前后、初始化前后以及销毁前。

## 1. 准备工作
我们先准备如下代码：
```java
public class BeanPostProcessor {
    public static void main(String[] args) {
        // GenericApplicationContext是一个“干净”的容器
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("bean1", Bean1.class);
        context.registerBean("bean1", Bean2.class);
        context.registerBean("bean1", Bean3.class);

        context.refresh();
        context.close();
    }
}
```
```java
@Component
public class Bean1 {
    private static final Logger log = LoggerFactory.getLogger(Bean1.class);
    private Bean2 bean2;
    private Bean3 bean3;
    private String home;

    @Autowired
    public void setBean2(Bean2 bean2) {
        log.info("@Autowired生效：{}", bean2);
        this.bean2 = bean2;
    }

    @Resource
    public void setBean3(Bean3 bean3) {
        log.info("@Resource生效：{}", bean3);
        this.bean3 = bean3;
    }

    @Autowired
    public void setHome(@Value("${JAVA_HOME") String home) {
        log.info("@Value生效：{}", home);
        this.home = home;
    }

    @PostConstruct public void init() {log.info("@PostConstruct生效");}
    @PreDestroy public void destroy() {log.info("@PreDestroy生效");}
}
```
```java
@Component public class Bean2 { }
```
```java
@Component public class Bean3 { }
```

这里我们用到了`GenericApplicationContext`作为容器。使用它作为容器是因为它是一个”干净“的容器，不带有任何后处理器，方便我们观察；同时功能完善，不像单纯的`BeanFactory`还需要我们手动添加`BeanDefinition`、手动执行后处理器，使用起来非常麻烦。

`refresh`方法用于初始化容器。在执行`refresh`方法后，容器会执行`BeanFactory`后处理器、添加`Bean`后处理器并初始化所有单例。

另外，在`Bean1`中，`@Autowired`与`@Value`配合用于**值注入**。Spring会自动去系统或者环境变量中寻找`JAVA_HOME`的变量，并将其值注入于`home`变量。

## 2. Bean后处理器
### 2.1. 解析`@Autowired`和`@Value`
配置好后尝试运行一次`main`方法，会发现没有打印任何有效信息，说明注解完全没有被解析。这是很显然的，因为容器里什么后处理器都没有，自然是没有办法进行注解解析功能。

我们首先加入解析`@Autowired`的Bean后处理器：`AutowiredAnnotationBeanPostProcessor`。当然这个Bean后处理器还能解析`@Value`注解。
```java
// 解析@Autowired和@Value
context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
```
运行会发现报错，原因跟`String`有关：
```aiignore
Exception in thread "main" org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'bean1': Unsatisfied dependency expressed through method 'setHome' parameter 0: No qualifying bean of type 'java.lang.String' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {@org.springframework.beans.factory.annotation.Value("${JAVA_HOME}")}
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredMethodElement.resolveMethodArguments(AutowiredAnnotationBeanPostProcessor.java:896)
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredMethodElement.inject(AutowiredAnnotationBeanPostProcessor.java:849)
	at org.springframework.beans.factory.annotation.InjectionMetadata.inject(InjectionMetadata.java:146)
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessProperties(AutowiredAnnotationBeanPostProcessor.java:509)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.populateBean(AbstractAutowireCapableBeanFactory.java:1445)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600)
	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:523)
	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:339)
	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:346)
	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:337)
	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:202)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.instantiateSingleton(DefaultListableBeanFactory.java:1155)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingleton(DefaultListableBeanFactory.java:1121)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:1056)
	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:987)
	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:627)
	at spring.bean.bean_post_processor.BeanPostProcessor.main(BeanPostProcessor.java:22)
Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'java.lang.String' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {@org.springframework.beans.factory.annotation.Value("${JAVA_HOME}")}
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.raiseNoMatchingBeanFound(DefaultListableBeanFactory.java:2177)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.doResolveDependency(DefaultListableBeanFactory.java:1627)
	at org.springframework.beans.factory.support.DefaultListableBeanFactory.resolveDependency(DefaultListableBeanFactory.java:1552)
	at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredMethodElement.resolveMethodArguments(AutowiredAnnotationBeanPostProcessor.java:888)
	... 16 more
```
这是因为在值注入的时候，我们必须还要设置另一个后处理器：
```java
context.getDefaultListableBeanFactory().setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
```
该后处理器在后面的章节会介绍，现在先简单地知道它能够处理值注入的情况即可。现在就能正常运行了，并且能看到`@Autowired`和`@Value`都被解析了：
```aiignore
15:22:52.940 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Autowired生效：spring.bean.bean_post_processor.Bean2@971d0d8
15:22:52.965 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Value生效：D:\Develop\Java\jdk-17
```

### 2.2. 解析`@Resource`、`@PostConstruct`和`PreDestroy`
既然`@Resource`还没有被解析，我们可以添加`CommonAnnotationBeanPostProcessor`这个Bean后处理器。
```java
// 解析@Resource，@PostConstruct和@PreDestory
context.registerBean(CommonAnnotationBeanPostProcessor.class);
```
但这个Bean后处理器不仅仅会解析`@Resource`，还会解析`@PostConstruct`和`@PreDestroy`：
```aiignore
15:26:52.730 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Resource生效：spring.bean.bean_post_processor.Bean3@c540f5a
15:26:52.740 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Autowired生效：spring.bean.bean_post_processor.Bean2@568bf312
15:26:52.759 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Value生效：D:\Develop\Java\jdk-17
15:26:52.760 [main] INFO spring.bean.bean_post_processor.Bean1 -- @PostConstruct生效
15:26:52.767 [main] INFO spring.bean.bean_post_processor.Bean1 -- @PreDestroy生效
```
可以看到这些注解现在都生效了。另外`@Resource`先于`@Autowired`生效，原因在[容器实现](Frameworks/src/main/java/spring/bean/ContextImplementation.md)的3.2节中已经详细讲解。

### 2.3. 解析`@ConfigurationProperties`
Spring Boot中有一个常见的注解`@ConfigurationProperties`。它的作用是让Spring Boot在启动时，把外部配置(`application.properties` / `application.yml`)中以指定前缀开头的属性，自动绑定到这个POJO字段中，形成一个类型安全、可注入的配置对象：
```java
@Data
@EnableConfigurationProperties(Bean4.class)
@ConfigurationProperties(prefix = "java")
public class Bean4 {
    private String home;
    private String version;
}
```
这个类会自动找到`java.home=`和`java.version=`两个键，然后读取值并绑定到两个字段中。当然在这个例子中，由于环境变量已经有这两个键值对了，所以不需要额外的配置文件。也就是说，我们会希望`home`和`version`所绑定的值与下面两个语句的值是相同的：
```java
System.getProperty("java.home"); // 应当与home字段的值相同
System.getProperty("java.version"); // 应当与version字段的值相同
```
为了解析这个注解，我们需要Bean后处理器`ConfigurationPropertiesBindingPostProcessor`。注册这个Bean后处理器的方式有些不同：
```java
ConfigurationPropertiesBindingPostProcessor.register(context.getDefaultListableBeanFactory());
```
在`context.refresh()`后，我们可以从容器中获取`Bean4`，打印查看它是否已经成功绑定了值：
```java
System.out.println(context.getBean(Bean4.class));
```
打印查看结果，一切符合我们的预期：
```aiignore
系统java.home = D:\Develop\Java\jdk-24.0.1
系统java.version = 24.0.1
15:50:31.003 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Resource生效：spring.bean.bean_post_processor.Bean3@5b0abc94
15:50:31.012 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Autowired生效：spring.bean.bean_post_processor.Bean2@1a84f40f
15:50:31.030 [main] INFO spring.bean.bean_post_processor.Bean1 -- @Value生效：D:\Develop\Java\jdk-17
15:50:31.032 [main] INFO spring.bean.bean_post_processor.Bean1 -- @PostConstruct生效
Bean4(home=D:\Develop\Java\jdk-24.0.1, version=24.0.1) // 与System#getProperty输出的内容相同
15:50:31.139 [main] INFO spring.bean.bean_post_processor.Bean1 -- @PreDestroy生效
```

## 3. Bean后处理器生效时间点
### 3.1. `AutowiredAnnotationBeanPostProcessor`
该Bean后处理器解析了`@Autowired`和`@Value`注解，说明它的作用时机在依赖注入之前。
```aiignore
实例化 → (*)依赖注入 → 初始化 → 销毁
```
`(*)`表示作用时机。
### 3.2. `CommonAnnotationBeanPostProcessor`
该Bean后处理器解析了`@Resource`、`@PostConstruct`和`@PreDestroy`，说明它的作用时机有三个：
1. 依赖注入前
2. 初始化前（注意，`@PostConstruct`字面意思是创建后，作用是在初始化前，也就是`init`方法前）
3. 销毁前

```aiignore
实例化 → (*)依赖注入 → (*)初始化 → (*)销毁
```