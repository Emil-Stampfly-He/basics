# 单例模式

## 1. Java中的单例模式
### 1.1. 使用枚举替代传统单例模式
自从Java的枚举`enum`得到广泛运用后，单例模式已经显得有些多余了，毕竟枚举是天然的单例模式——能够避免构造方法被反射调用且是多线程安全的：

```java
public enum Singleton {
    INSTANCE;
    
    private final Properties properties; // 字段

    // 构造方法天然是private，且无法被反射调用
    Singleton() {
        this.properties = new Properties();
    } 
    
    // 对外暴露的方法
    public String getProperty(String key) { return this.properties.getProperty(key);}
    public void setProperties(String key, String value) {this.properties.setProperty(key, value);}
}
```

但仍有很多以前的库使用传统的单例模式——私有化构造方法并使用静态方法获取唯一实例：
```java
public class Singleton {
    private final Singleton SINGLETON = Singleton.getInstance();
    
    private Singleton() {}
    public static Singleton getInstance() {return new Singleton();}
}
```

### 1.2. 传统单例的应用场景：懒初始化
如果单例构造的开销很大，希望在真正使用时再加载，则可以使用静态内部类的方式（`Holder`模式）：
```java
public class LazySingleton {
    private LazySingleton() { /*...*/ }
    public static LazySingleton getInstance() {return Holder.INSTANCE;}
    
    private static class Holder {
        static final LazySingleton INSTANCE = new LazySingleton();
    }
}
```

## 2. Rust中的单例模式
Rust中单例模式并不被推荐，甚至可能成为一种“反模式”。经典单例通常伴随全局可变状态（`static mut`），这在Rust中是不可接受的。如果真的需要全局单例，则一般使用`LazyLock`或者`lazy_static`（懒加载）。例如以下是模仿Redis中的全局TTL哈希表，它既是一个懒加载单例，又是多线程安全的单例：
```rust
static EXPIRE_MAP: LazyLock<Arc<Mutex<HashMap<(String, u64), u64>>>> = LazyLock::new(|| {
    // key: (key, ttl) value: created time
    Arc::new(Mutex::new(HashMap::new()))
});
```