package command.command;

import command.furniture.Stereo;

public class StereoOnWithRadioCommand extends StereoOnAbstractCommand {

    public StereoOnWithRadioCommand(Stereo stereo) {
        super(stereo);
    }

    @Override
    public void execute() {
        this.stereo.on();
        this.stereo.setRadio();
        this.stereo.setVolume(11);
        System.out.println("Stereo on with radio, volume 11");
    }
}
