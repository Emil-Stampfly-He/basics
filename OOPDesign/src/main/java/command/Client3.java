package command;

import command.command.*;
import command.furniture.Light;
import command.furniture.Stereo;

public class Client3 {
    public static void main(String[] args) {
        RemoteControl remoteControl = new RemoteControl();

        Light light = new Light("Living Room");
        Stereo stereo = new Stereo();

        LightOnCommand lightOnCommand = new LightOnCommand(light);
        LightOffCommand lightOffCommand = new LightOffCommand(light);
        StereoOnWithCDCommand stereoOnWithCDCommand = new StereoOnWithCDCommand(stereo);
        StereoOffCommand stereoOffCommand = new StereoOffCommand(stereo);

        MacroCommand partyOn = new MacroCommand(lightOnCommand, stereoOnWithCDCommand);
        MacroCommand partyOff = new MacroCommand(lightOffCommand, stereoOffCommand);

        remoteControl.setCommand(0, partyOn, partyOff);

        System.out.println(remoteControl);
        System.out.println("----- Pushing Macro On Command -----");
        remoteControl.onButtonWasPushed(0);

        System.out.println("----- Pushing Macro Off Command -----");
        remoteControl.offButtonWasPushed(0);

        System.out.println("----- Undo -----");
        remoteControl.undoButtonWasPushed();
    }
}
