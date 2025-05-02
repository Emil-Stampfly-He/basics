package proxy.jdk;

public class Main {

    interface Foo {
        void foo();
        int bar();
    }

    static class Target implements Foo {
        @Override
        public void foo() {
            System.out.println("target foo");
        }

        @Override
        public int bar() {
            System.out.println("target bar");
            return 100;
        }
    }

    public static void main(String[] args) {
        Foo proxy = new $Proxy0((proxy1, method, params) -> {
            System.out.println("before...");
            return method.invoke(new Target(), params);
        });

        proxy.foo();
        int bar = proxy.bar();
        System.out.println(bar);
    }
}
