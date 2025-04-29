package application;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class Main {

    private static volatile boolean running = true;

    public static void main(String[] args) throws InterruptedException {
        int x = 3;
        int y = 3;
        //CountDownLatch startSign = new CountDownLatch(1);
        ParkingLotDispatcher dispatcher = new ParkingLotDispatcher(x, y);
        BlockingQueue<Car> waitingQueue = dispatcher.getWaitingQueue();
        Thread producer = getProducerThread(x, y, waitingQueue);

        producer.start();
        dispatcher.start();
        running = false;
        producer.join();
    }

    private static Thread getProducerThread(int x, int y, BlockingQueue<Car> waitingQueue) {
        ConcurrentHashMap<Long, Car> unparkedCars = new ConcurrentHashMap<>(x * y);
        CopyOnWriteArrayList<Car> parkedCars = new CopyOnWriteArrayList<>();
        for (int i = 0; i < x * y; i++) {
            Car car = new Car();
            car.setId(i);
            unparkedCars.put(car.getId(), car);
        }

        return new Thread(() -> {
            Random rand = new Random();
            while (running) {
                try {
                    if (parkedCars.size() == x * y) break;

                    int id = rand.nextInt(x * y);
                    Car unparkedCar = unparkedCars.get((long) id);
                    if (unparkedCar == null) continue;

                    waitingQueue.put(unparkedCar);
                    parkedCars.add(unparkedCar);
                    unparkedCars.remove((long) id);

                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
    }
}
