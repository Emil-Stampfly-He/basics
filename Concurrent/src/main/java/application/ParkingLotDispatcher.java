package application;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerArray;

@Slf4j
@SuppressWarnings("all")
@Data
public class ParkingLotDispatcher {

    private final ConcurrentHashMap<Car, Position> parkedCars;
    private final BlockingQueue<Car> waitingQueue;
    private final ExecutorService pool;
    private final Random random;
    private final int x;
    private final int y;
    private final int nThread;
    private final AtomicIntegerArray[] parkingPlace;

    public ParkingLotDispatcher(int x, int y, int nThread) {
        this.x = x;
        this.y = y;
        this.parkingPlace = new AtomicIntegerArray[x];
        // All elements are 0 automatically
        for (int row = 0; row < x; row++) {
            parkingPlace[row] = new AtomicIntegerArray(y);
        }

        this.nThread = nThread;
        this.pool = Executors.newFixedThreadPool(nThread);
        this.parkedCars = new ConcurrentHashMap<>(x * y);
        this.waitingQueue = new ArrayBlockingQueue<>(x * y);
        this.random = new Random();
    }

    public Duration start() throws InterruptedException {
        long start = System.nanoTime();

        for (int i = 0; i < nThread; i++) {
            pool.execute(() -> {
                try {

                    while (true) {
                        if (parkedCars.size() == x * y && waitingQueue.isEmpty()) break;

                        // Dispatch a parking lot to a queueing car
                        Car unparkedCar = waitingQueue.take();
                        if (unparkedCar == null) continue;

                        while (true) {
                            int xP = random.nextInt(this.x);
                            int yP = random.nextInt(this.y);

                            if (parkingPlace[xP].compareAndSet(yP, 0, 1)) {
                                unparkedCar.setParkingLot(new Position(xP, yP));
                                parkedCars.put(unparkedCar, unparkedCar.getParkingLot());
                                log.info("Finished dispatching Car {}. Parking Lot: ({}, {})", unparkedCar.getId(), xP, yP);
                                break;
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }

        pool.shutdown();

        long end = System.nanoTime();
        long elapsed = (end - start) / 1_000_000; // of ms
        log.info("End");

        return Duration.ofMillis(elapsed);
    }
}

