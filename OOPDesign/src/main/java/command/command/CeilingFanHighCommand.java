package command.command;

import command.furniture.CeilingFan;

public class CeilingFanHighCommand extends CeilingFanAbstractCommand {

    public CeilingFanHighCommand(CeilingFan ceilingFan) {
        super(ceilingFan);
    }

    @Override
    public void execute() {
        this.previousSpeedLevel = this.ceilingFan.getSpeedLevel();
        this.ceilingFan.high();
    }
}
