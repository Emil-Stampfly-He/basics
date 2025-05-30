package command;

import command.command.Command;
import command.command.NoCommand;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

public class RemoteControl {
    Command[] onCommands;
    Command[] offCommands;
    Deque<Command> historyCommands = new ArrayDeque<>();

    public RemoteControl() {
        this.onCommands = new Command[7];
        this.offCommands = new Command[7];

        // null command, does nothing
        Command noCommand = new NoCommand();
        for (int i = 0; i < this.onCommands.length; i++) {
            this.onCommands[i] = noCommand;
            this.offCommands[i] = noCommand;
        }
    }

    public void setCommand(int slot, Command onCommand, Command offCommand) {
        this.onCommands[slot] = onCommand;
        this.offCommands[slot] = offCommand;
    }

    public void onButtonWasPushed(int slot) {
        this.onCommands[slot].execute();
        this.historyCommands.push(this.onCommands[slot]);
    }

    public void offButtonWasPushed(int slot) {
        this.offCommands[slot].execute();
        this.historyCommands.push(this.offCommands[slot]);
    }

    public void undoButtonWasPushed() {
        try {
            Command undoCommand = this.historyCommands.pop();
            undoCommand.undo();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No more commands to undo.");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n----- Remote Control -----\n");
        for (int i = 0; i < this.onCommands.length; i++) {
            sb.append("[slot ")
                    .append(i)
                    .append("]: ")
                    .append(this.onCommands[i].getClass().getName())
                    .append("    ")
                    .append(this.offCommands[i].getClass().getName())
                    .append("\n");
        }

        return sb.toString();
    }
}
