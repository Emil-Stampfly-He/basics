# 详解`@Autowired`的Bean后处理器

我们已经知道，解析`@Autowired`注解的Bean后处理器是`AutowiredAnnotationBeanPostProcessor`。这节便围绕这个Bean后处理器展开。

## 1.