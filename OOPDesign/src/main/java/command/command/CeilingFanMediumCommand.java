package command.command;

import command.furniture.CeilingFan;

public class CeilingFanMediumCommand extends CeilingFanAbstractCommand {

    public CeilingFanMediumCommand(CeilingFan ceilingFan) {
        super(ceilingFan);
    }

    @Override
    public void execute() {
        this.previousSpeedLevel = this.ceilingFan.getSpeedLevel();
        this.ceilingFan.medium();
    }
}
