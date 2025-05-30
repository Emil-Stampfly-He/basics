package command.command;

import command.furniture.Stereo;

public abstract class StereoOnAbstractCommand implements Command {

    protected final Stereo stereo;

    public StereoOnAbstractCommand(Stereo stereo) {
        this.stereo = stereo;
    }

    @Override
    public abstract void execute();

    @Override
    public void undo() {
        this.stereo.off();
        System.out.println("Stereo off");
    }

}
