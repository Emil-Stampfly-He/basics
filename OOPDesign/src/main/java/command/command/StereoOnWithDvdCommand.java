package command.command;

import command.furniture.Stereo;

public class StereoOnWithDvdCommand extends StereoOnAbstractCommand {

    public StereoOnWithDvdCommand(Stereo stereo) {
        super(stereo);
    }

    @Override
    public void execute() {
        this.stereo.on();
        this.stereo.setDvd();
        this.stereo.setVolume(11);
        System.out.println("Stereo on with DVD, volume 11");
    }
}
