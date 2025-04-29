package virtual_thread;

public class VirtualThreadDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = Thread.ofVirtual().start(() -> System.out.println("Hello World"));
        thread.join();
    }
}
