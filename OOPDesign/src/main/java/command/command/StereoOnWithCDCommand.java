package command.command;

import command.furniture.Stereo;

public class StereoOnWithCDCommand extends StereoOnAbstractCommand {

    public StereoOnWithCDCommand(Stereo stereo) {
        super(stereo);
    }

    @Override
    public void execute() {
        this.stereo.on();
        this.stereo.setCd();
        this.stereo.setVolume(11);
        System.out.println("Stereo on with CD, volume 11");
    }
}
