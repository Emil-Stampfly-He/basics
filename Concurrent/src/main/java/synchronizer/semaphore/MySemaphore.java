package synchronizer.semaphore;

public class MySemaphore {

    private int permits; // 许可数

    public MySemaphore(int permits) {
        if (permits < 0) {
            throw new IllegalArgumentException("Permits cannot be negative.");
        }

        this.permits = permits;
    }
}
