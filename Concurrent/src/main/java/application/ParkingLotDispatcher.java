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
    private final Random random;
    private final int x;
    private final int y;
    private final AtomicIntegerArray[] parkingPlace;

    public ParkingLotDispatcher(int x, int y) {
        this.x = x;
        this.y = y;
        this.parkingPlace = new AtomicIntegerArray[x];
        // All elements are 0 automatically
        for (int row = 0; row < x; row++) {
            parkingPlace[row] = new AtomicIntegerArray(y);
        }

        this.parkedCars = new ConcurrentHashMap<>(x * y);
        this.waitingQueue = new ArrayBlockingQueue<>(x * y);
        this.random = new Random();
    }

    public Duration start() {
        long start = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(5); // 5 dispatchers

        try {
            for (int i = 0; i < 5; i++) {
                pool.execute(() -> {
                    try {
                        while (true) {
                            if (parkedCars.size() == x * y && waitingQueue.isEmpty()) break;

                            // Dispatch a parking lot to a queueing car
                            Car unparkedCar = waitingQueue.poll(1, TimeUnit.SECONDS);
                            if (unparkedCar == null) continue;

                            while (true) {
                                int xP = random.nextInt(this.x);
                                int yP = random.nextInt(this.y);

                                if (parkingPlace[xP].compareAndSet(yP, 0, 1)) {
                                    unparkedCar.setParkingLot(new Position(x, y));
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
            if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        long end = System.nanoTime();
        long elapsed = (end - start) / 1_000_000; // of ms
        log.info("End");

        return Duration.ofMillis(elapsed);
    }
}

