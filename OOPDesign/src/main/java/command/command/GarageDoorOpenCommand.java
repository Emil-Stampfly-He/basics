package command.command;

import command.furniture.GarageDoor;

public class GarageDoorOpenCommand implements Command {

    private final GarageDoor garageDoor;

    public GarageDoorOpenCommand(GarageDoor garageDoor) {
        this.garageDoor = garageDoor;
    }

    @Override
    public void execute() {
        this.garageDoor.up();
        this.garageDoor.lightOn();
    }

    @Override
    public void undo() {
        this.garageDoor.down();
        this.garageDoor.lightOff();
    }
}
