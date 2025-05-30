package command.command;

import command.furniture.CeilingFan;

public class CeilingFanOffCommand extends CeilingFanAbstractCommand{

    public CeilingFanOffCommand(CeilingFan ceilingFan) {
        super(ceilingFan);
    }

    @Override
    public void execute() {
        this.previousSpeedLevel = this.ceilingFan.getSpeedLevel();
        this.ceilingFan.off();
    }
}
