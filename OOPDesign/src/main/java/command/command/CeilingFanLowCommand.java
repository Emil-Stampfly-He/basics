package command.command;

import command.furniture.CeilingFan;

public class CeilingFanLowCommand extends CeilingFanAbstractCommand{

    public CeilingFanLowCommand(CeilingFan ceilingFan) {
        super(ceilingFan);
    }

    @Override
    public void execute() {
        this.previousSpeedLevel = this.ceilingFan.getSpeedLevel();
        this.ceilingFan.low();
    }
}
