package command;

import command.command.*;
import command.furniture.GarageDoor;
import command.furniture.Light;
import command.furniture.Stereo;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Client1 {
    public static void main(String[] args) {
        RemoteControl remoteControl = new RemoteControl();

        List<Consumer<RemoteControl>> tasks = List.of(
                rc -> {
                    Light kitchen = new Light("Kitchen");
                    executeCommand(rc, 0,
                            new LightOnCommand(kitchen),
                            new LightOffCommand(kitchen));

                    Light livingRoom = new Light("Living Room");
                    executeCommand(rc, 0,
                            new LightOnCommand(livingRoom),
                            new LightOffCommand(livingRoom));
                },
                rc -> {
                    GarageDoor door = new GarageDoor();
                    executeCommand(rc, 1,
                            new GarageDoorOpenCommand(door),
                            new GarageDoorOffCommand(door));
                },
                rc -> {
                    Stereo stereo = new Stereo();
                    executeCommand(rc, 2,
                            new StereoOnWithCDCommand(stereo),
                            new StereoOffCommand(stereo));
                }
        );

        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        tasks.forEach(task -> executor.submit(() -> task.accept(remoteControl)));

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("Some tasks failed due to running out of time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(remoteControl);
    }

    private static void executeCommand(
            RemoteControl remote, int slot,
            Command onCommand, Command offCommand) {
        remote.setCommand(slot, onCommand, offCommand);
        remote.onButtonWasPushed(slot);
        remote.offButtonWasPushed(slot);
        System.out.println(remote);
        remote.undoButtonWasPushed();
    }
}

