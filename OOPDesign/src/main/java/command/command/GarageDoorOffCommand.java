package command.command;

import command.furniture.GarageDoor;

public class GarageDoorOffCommand implements Command {

    private final GarageDoor garageDoor;

    public GarageDoorOffCommand(GarageDoor garageDoor) {
        this.garageDoor = garageDoor;
    }

    @Override
    public void execute() {
        this.garageDoor.down();
        this.garageDoor.lightOff();
    }

    @Override
    public void undo() {
        this.garageDoor.up();
        this.garageDoor.lightOn();
    }
}
