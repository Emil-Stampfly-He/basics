# 从零开始的`CountDownLatch`

**Github: https://github.com/Emil-Stampfly-He/basics**

我们首先回顾一下`CountDownLatch`的作用：
1. 它维护了一个计数器，其初始值由构造器指定
2. 调用`await`的线程在计数器变成0之前会被阻塞
3. 每调用一次`countDown`，计数器就会减1
4. 计数器变成0时，所有阻塞的线程都会被唤醒

## 1. 基本构造
从上述的功能中可以看出，我们需要一个`count`字段当作计数器。这个计数器的值必须大于0才有意义：
```Java
public class MyCountDownLatch {
    private int count;

    public MyCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative.");
        }
        
        this.count = count;
    }
}
```