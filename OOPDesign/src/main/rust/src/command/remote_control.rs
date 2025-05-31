use crate::command::command::{Command, NoCommand};

pub struct RemoteControl {
    history: Vec<Box<dyn Command>>,
    on_commands: Vec<Box<dyn Command>>,
    off_commands: Vec<Box<dyn Command>>,
}

impl RemoteControl {
    pub fn new() -> Self {
        let mut on_commands_vec: Vec<Box<dyn Command>> = Vec::with_capacity(7);
        let mut off_command_vec: Vec<Box<dyn Command>> = Vec::with_capacity(7);
        for _ in 0..7 {
            on_commands_vec.push(Box::new(NoCommand::new()) as Box<dyn Command>);
            off_command_vec.push(Box::new(NoCommand::new()) as Box<dyn Command>);
        }

        Self {
            history: Vec::new(),
            on_commands: on_commands_vec,
            off_commands: off_command_vec,
        }
    }

    pub fn set_commands(&mut self,
                        slot: usize,
                        on_command: Box<dyn Command>,
                        off_command: Box<dyn Command>) {
        self.on_commands[slot] = on_command;
        self.off_commands[slot] = off_command;
    }

    pub fn on_pressing_on_button(&mut self, slot: usize) {
        let mut on_command = self.on_commands[slot].clone();
        on_command.execute();
        self.history.push(on_command);
    }

    pub fn on_pressing_off_button(&mut self, slot: usize) {
        let mut off_command = self.off_commands[slot].clone();
        off_command.execute();
        self.history.push(off_command);
    }

    pub fn on_pressing_undo_button(&mut self) {
        match self.history.pop() {
            Some(mut command) => {command.undo()}
            None => {panic!("Nothing to undo")}
        }
    }

    pub fn to_string(&self) {
        println!("------ Remote Control ------");
        for i in 0..self.on_commands.len() {
            let on_name  = self.on_commands[i].name();
            let off_name = self.off_commands[i].name();
            println!("[slot {}]: on = {}, off = {}", i, on_name, off_name);
        }
    }
}
