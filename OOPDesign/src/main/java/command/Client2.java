package command;

import command.command.CeilingFanHighCommand;
import command.command.CeilingFanMediumCommand;
import command.command.CeilingFanOffCommand;
import command.furniture.CeilingFan;

public class Client2 {
    public static void main(String[] args) {
        RemoteControl remoteControl = new RemoteControl();

        CeilingFan livingRoomFan = new CeilingFan("Living Room");
        CeilingFanMediumCommand ceilingFanMediumCommand = new CeilingFanMediumCommand(livingRoomFan);
        CeilingFanHighCommand ceilingFanHighCommand = new CeilingFanHighCommand(livingRoomFan);
        CeilingFanOffCommand ceilingFanOffCommand = new CeilingFanOffCommand(livingRoomFan);

        System.out.println(livingRoomFan.getSpeedLevel());

        remoteControl.setCommand(0, ceilingFanMediumCommand, ceilingFanOffCommand);
        remoteControl.setCommand(1, ceilingFanHighCommand, ceilingFanOffCommand);

        remoteControl.onButtonWasPushed(0);
        System.out.println("On: " + livingRoomFan.getSpeedLevel()); // MEDIUM

        remoteControl.offButtonWasPushed(0);
        System.out.println("Off: " + livingRoomFan.getSpeedLevel()); // OFF

        remoteControl.undoButtonWasPushed();
        System.out.println("Undo: " + livingRoomFan.getSpeedLevel()); // MEDIUM

        remoteControl.onButtonWasPushed(1);
        System.out.println("On: " + livingRoomFan.getSpeedLevel()); // HIGH

        remoteControl.undoButtonWasPushed();
        System.out.println("Undo: " + livingRoomFan.getSpeedLevel()); // MEDIUM

        remoteControl.undoButtonWasPushed();
        System.out.println("Undo: " + livingRoomFan.getSpeedLevel());  // OFF

        remoteControl.undoButtonWasPushed();
        System.out.println("Undo: " + livingRoomFan.getSpeedLevel()); // ERROR
    }
}
