# 容器实现

## 1. `BeanFactory`的创建
`BeanFactory`最重要的一个实现类是我们前面已经见过的`DefaultBlistableBeanFactory`：
```java
public class TestBeanFactory {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    }
}
```
但创建完容器后，这个容器还什么都没有。那么首先我们就需要添加一些Bean的定义（`BeanDefinition`），让容器知道怎么去创建一个Bean。
为了明确一个Bean的定义，我们需要指明Bean的`class`、scope（是单例还是多例）、初始化以及销毁，然后再使用`registerBeanDefinition`方法，将这个定义注册到容器中：
```java
public class TestBeanFactory {
    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 1. Bean的定义（BeanDefinition）
        // class, scope（单例/多例）, 初始化, 销毁
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(Config.class)
                .setScope("singleton")
                .getBeanDefinition();
        beanFactory.registerBeanDefinition("config", beanDefinition);

        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
    }

    @Configuration
    static class Config {
        @Bean public Bean1 bean1() {return new Bean1();}
        @Bean public Bean2 bean2() {return new Bean2();}
    }

    @Slf4j
    static class Bean1 {
        @Autowired private Bean2 bean2;
        public Bean1() {log.info("构造Bean1");}
        public Bean2 getBean2() {return bean2;}
    }

    @Slf4j
    static class Bean2 { public Bean2() {log.info("构造Bean2");}}
}
```
`Config`、`Bean1`和`Bean2`是我们准备的配置类和Bean，其中Bean1依赖于Bean2的注入。我们在使用Spring框架的时候，如果Spring遇到一个带有`@Configuration`的配置类，那么Spring会自动将其中带有`@Bean`注解的方法解析出来，并将方法中返回的对象作为Bean注入于容器中。
那么，我们在向`beanFactory`中注册了`BeanDefinition`之后，应当能够解析出`Config`下面的类。输出查看结果：
```aiignore
config
```
结果却只输出了一个`config`，`beanFactory`并没有解析出`Config`类下的两个Bean。这说明目前为止我们创建的`beanFactory`并没有解析Bean的功能。那么这个功能是谁提供的呢？

## 2. 后处理器
答案是后处理器。我们调用`AnnotationConfigUtils`这个工具类中的静态方法`registerAnnotationConfigProcessors`后，`beanFactory`中就会被添加一些后处理器：
```java
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

    for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
        System.out.println(beanDefinitionName);
    }
}
```
只有在`beanFactory`中拥有了后处理器的扩展功能时，它才能够去解析相关的注解（`@Configuration` & `@Bean`）。
现在我们再来打印一下：
```aiignore
config
org.springframework.context.annotation.internalConfigurationAnnotationProcessor
org.springframework.context.annotation.internalAutowiredAnnotationProcessor
org.springframework.context.annotation.internalCommonAnnotationProcessor
org.springframework.context.event.internalEventListenerProcessor
org.springframework.context.event.internalEventListenerFactory
```
除了先前我们自己加入的`config`外，容器中还多了5个Bean。我们只关注前3个：
* `internalConfigurationAnnotationProcessor`：BeanFactory后处理器。用于处理`@Configuration`注解和其中的`@Bean`注解
* `internalAutowiredAnnotationProcessor`：Bean后处理器。用于处理`@Autowired`注解
* `internalCommonAnnotationProcessor`：Bean后处理器。用于处理`@Resource`注解
> `@Resource`的功能与`@Autowired`相同，用于依赖注入，只不过是由JakartaEE提供的注解。

### 2.1. BeanFactory后处理器
虽然容器中已经有了这些后处理器，但它们还没有工作。首先我们要从`@Configuration`中解析出`@Bean`才行，也就是让`internalConfigurationAnnotationProcessor`开始工作。
`internalConfigurationAnnotationProcessor`来自`ConfigurationClassPostProcessor`类，该类间接实现了`BeanFactoryPostProcessor`接口，所以说是BeanFactory后处理器而不是Bean后处理器。我们
可以通过`BeanFactoryPostProcessor.class`从容器中拿到这个Bean`internalConfigurationAnnotationProcessor`，并执行对应类下的`postProcessBeanFactory`方法：
```java
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
            .forEach(beanFactoryPostProcessor -> beanFactoryPostProcessor.postProcessBeanFactory(beanFactory));

    for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
        System.out.println(beanDefinitionName);
    }
}
```
打印后，我们可以发现`@Configuration`和`@Bean`都被解析了：
```aiignore
config
org.springframework.context.annotation.internalConfigurationAnnotationProcessor
org.springframework.context.annotation.internalAutowiredAnnotationProcessor
org.springframework.context.annotation.internalCommonAnnotationProcessor
org.springframework.context.event.internalEventListenerProcessor
org.springframework.context.event.internalEventListenerFactory
bean1
bean2
```

### 2.2. Bean后处理器
既然`Bean1`已经被注入容器，我们来看看是否可以通过`Bean1`获取`Bean2`:
```java
System.out.println(beanFactory.getBean(Bean1.class).getBean2());
```
```aiignore
10:06:15.911 [main] INFO spring.bean.TestBeanFactory$Bean1 -- 构造Bean1
null
```
`Bean1`确实可以被获取，但是`getBean2`方法输出为`null`，这说明`@Autowired`的注解并没有生效。

这是因为`BeanFactory`同样没有自带依赖注入的功能，需要Bean后处理器进行功能的扩展.Bean后处理器会针对Bean的生命周期的各个阶段提供扩展，例如：
* `internalAutowiredAnnotationProcessor`：解析`@Aotuwired`
* `internalCommonAnnotationProcessor`：解析`@Resource`

因此，我们需要让`beanFactory`中的Bean后处理器工作起来：
```java
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
            .forEach(beanFactoryPostProcessor -> beanFactoryPostProcessor.postProcessBeanFactory(beanFactory));

    // 4. Bean后处理器，针对Bean的生命周期的各个阶段提供扩展，例如@Autowired
    beanFactory.getBeansOfType(BeanPostProcessor.class).values()
            .forEach(beanFactory::addBeanPostProcessor);

    for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
        System.out.println(beanDefinitionName);
    }

    System.out.println();

    System.out.println(beanFactory.getBean(Bean1.class).getBean2());
}
```
这下我们应该能够从`Bean1`中拿到被注入的`Bean2`了：
```aiignore
10:26:06.638 [main] INFO spring.bean.TestBeanFactory$Bean1 -- 构造Bean1
10:26:06.649 [main] INFO spring.bean.TestBeanFactory$Bean2 -- 构造Bean2
spring.bean.TestBeanFactory$Bean2@5c33f1a9
```
> 补充说明：
> ```java
> beanFactory.getBeansOfType(BeanPostProcessor.class).values()
>       .forEach(beanFactory::addBeanPostProcessor);
> ```
> 这里的`addBeanPostProcessor`更准确的描述是真正让Bean后处理器工作起来，而不是“又”添加了一遍Bean后处理器。

### 2.3. `preInstantiateSingletons`
实际上，`Bean1`和`Bean2`这两个Bean都是在用到的时候才创建的。实际上Spring启动会提前把所有Bean都预先创建好，这样在使用的时候就不会再临时创建了：
```java
beanFactory.preInstantiateSingletons();
```
我们对比一下使用和不使用这个`preInstantiateSingletons`的区别：
```java
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
            .forEach(beanFactoryPostProcessor -> beanFactoryPostProcessor.postProcessBeanFactory(beanFactory));

    // 4. Bean后处理器，针对Bean的生命周期的各个阶段提供扩展，例如@Autowired
    beanFactory.getBeansOfType(BeanPostProcessor.class).values()
            .forEach(beanFactory::addBeanPostProcessor);

    for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
        System.out.println(beanDefinitionName);
    }
    
    // 5. 提前创建好单例Bean
    beanFactory.preInstantiateSingletons();
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>");
    System.out.println(beanFactory.getBean(Bean1.class).getBean2());
}
```
如果不使用，那么“创建`Bean1`之类的信息会在分割线下方出现。如果使用，那么这些创建信息会在分割线上方出现：
```aiignore
10:49:21.762 [main] INFO spring.bean.TestBeanFactory$Bean1 -- 构造Bean1
10:49:21.771 [main] INFO spring.bean.TestBeanFactory$Bean2 -- 构造Bean2
>>>>>>>>>>>>>>>>>>>>>>
spring.bean.TestBeanFactory$Bean2@1bd4fdd
```

## 3. `@Autowired`与`@Resourece`的优先级

### 3.1. `@Autowired`与`@Resource`的区别
假设我们要注入一个Bean，但是给这个Bean上既加了`@Autowired`又加了`@Resource`，那么Spring会使用哪个注解呢？

假设我们现在有以下代码（多了一个内部接口`Inter`和实现了这个接口的两个类`Bean3`和`Bean4`。我们使用`@Autowired`给`Bean1`注入一个`Inter`类型的Bean：
```java
@Slf4j
public class TestBeanFactory {
    public static void main(String[] args) {
        /*...*/
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
        @Autowired private Inter inter;

        public Bean1() {log.info("构造Bean1");}
        public Bean2 getBean2() {return bean2;}
        public Inter getInter() {return inter;}
    }

    @Slf4j
    static class Bean2 { public Bean2() {log.info("构造Bean2");}}
}
```
像这样`@Autowired private Inter inter;`来注入一个`Inter`类型的Bean，IDE就已经给我们报错了，因为实现了这个接口的Bean有两种，Spring并不能知道是注入`Bean3`还是`Bean4`。一种解决方案是加上`@Qualifier`：

```java
@Autowired @Qualifier("bean3") private Inter inter;
```
这样Spring就能够根据`@Qualifier`中指定的Bean名称去注入相应的Bean。但一般地，我们将上面这行改成下面这一行：
```java
@Autowired private Inter bean3;
```
这样Spring就能直接根据`bean3`来寻找`Bean3`并注入，不用多余的注解。

`@Resource`跟`@Autowired`非常相似，只不过可以给`@Resource`指定一个字符串的值。如果没有指定值，那么`@Resource`跟`@Autowired`一样，而如果指定了值，那么就相当于`@Autowired` + `@Qualifier`：
```java
// 不报错，注入bean3
@Resource("bean3") private Inter inter;
public Inter getInter() {return inter;}

// 不报错，注入bean4
@Resource(name = "bean4") private Inter bean3;
public Inter getInter() {return bean3;}

// 报错
@Resource private Inter inter;
public Inter getInter() {return inter;}
```

### 3.2. `@Autowired`叠加`@Resource`
现在我们将注入改成这样（现实中不会有人这么做）：
```java
@Autowired 
@Resource(name = "bean4") 
private Inter bean3;
```
那么下面的输出会是什么呢？
```java
System.out.println(beanFactory.getBean(Bean1.class).getInter());
```
答案是：
```aiignore
spring.bean.TestBeanFactory$Bean3@145f66e3
```
`@Autowired`生效而`@Resource`未生效（即使把`@Resource`放在`@Autowired`上面也一样，生效顺序与谁上谁下无关）。这说明`@Autowired`的Bean后处理器的优先级更高。
我们把`main`方法中的第4步改成下面的代码就可以查看谁先生效：
```java
// 4. Bean后处理器，针对Bean的生命周期的各个阶段提供扩展，例如@Autowired
beanFactory.getBeansOfType(BeanPostProcessor.class).values()
        .forEach(beanPostProcessor -> {
            System.out.println(beanPostProcessor.getClass().getName());
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        });
```
```aiignore
org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
```
确实是`@Autowired`后处理器先作用，所以`@Autowired`先生效，`@Resource`就被覆盖了。

### 3.3. 控制顺序
我们通过一个比较器来反转顺序：
```java
// 4. Bean后处理器，针对Bean的生命周期的各个阶段提供扩展，例如@Autowired
beanFactory.getBeansOfType(BeanPostProcessor.class).values()
        .stream()
        .sorted(beanFactory.getDependencyComparator())
        .forEach(beanPostProcessor -> {
            System.out.println(beanPostProcessor.getClass().getName());
            beanFactory.addBeanPostProcessor(beanPostProcessor);
        });
```
再查看结果，就能发现`@Resource`生效而`@Autowired`未生效了：
```aiignore
org.springframework.context.annotation.CommonAnnotationBeanPostProcessor
org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
...
spring.bean.TestBeanFactory$Bean4@799f10e1
```

## 4. `ApplicationContext`的实现
`BeanFactory`本身的功能十分有限，并不会主动调用BeanFactory后处理器、不会主动调用Bean后处理器，也不会主动初始化单例。
`BeanFactory`只是一个非常基础的容器，其他功能需要`ApplicationContext`来提供。