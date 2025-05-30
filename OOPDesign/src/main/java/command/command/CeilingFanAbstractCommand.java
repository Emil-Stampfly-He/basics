package command.command;

import command.furniture.CeilingFan;
import command.furniture.SpeedLevel;

public abstract class CeilingFanAbstractCommand implements Command {
    protected final CeilingFan ceilingFan;
    protected SpeedLevel previousSpeedLevel;

    protected CeilingFanAbstractCommand(CeilingFan ceilingFan) {
        this.ceilingFan = ceilingFan;
    }

    @Override
    public abstract void execute();

    @Override
    public void undo() {
        switch (this.previousSpeedLevel) {
            case LOW -> this.ceilingFan.low();
            case MEDIUM -> this.ceilingFan.medium();
            case HIGH -> this.ceilingFan.high();
            case OFF -> this.ceilingFan.off();
            default -> {}
        }
    }
}
