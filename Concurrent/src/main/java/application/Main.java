package application;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class Main {

    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        int x = 3;
        int y = 3;
        //CountDownLatch startSign = new CountDownLatch(1);
        ParkingLotDispatcher dispatcher = new ParkingLotDispatcher(x, y);
        BlockingQueue<Car> waitingQueue = dispatcher.getWaitingQueue();
        Thread producer = getThread(x, y, waitingQueue);

        producer.start();
        dispatcher.start();
        running = false;
        producer.join();
    }

    private static Thread getThread(int x, int y, BlockingQueue<Car> waitingQueue) {
        ConcurrentHashMap<Long, Car> unparkedCars = new ConcurrentHashMap<>(x * y);
        for (int i = 0; i < x * y; i++) {
            Car car = new Car();
            car.setId(i);
            unparkedCars.put(car.getId(), car);
        }

        return new Thread(() -> {
            Random rand = new Random();
            while (running) {
                try {

                    int id = rand.nextInt(x * y);
                    Car unparkedCar = unparkedCars.get((long) id);
                    if (unparkedCar == null) continue;

                    waitingQueue.put(unparkedCar);

                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
