package command.command;

import command.furniture.Stereo;

public class StereoOffCommand implements Command {
    Stereo stereo;

    public StereoOffCommand(Stereo stereo) {
        this.stereo = stereo;
    }

    @Override
    public void execute() {
        this.stereo.off();
        System.out.println("Stereo is off");
    }

    @Override
    public void undo() {
        this.stereo.on();
        System.out.println("Stereo is on");
    }
}
