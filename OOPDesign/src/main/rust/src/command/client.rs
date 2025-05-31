use std::cell::RefCell;
use std::rc::Rc;
use crate::command::command::{LightOffCommand, LightOnCommand};
use crate::command::furniture::Light;
use crate::command::remote_control::RemoteControl;

pub fn run_client() {
    let mut remote_control = RemoteControl::new();

    let light = Rc::new(RefCell::new(Light {
        on: false,
    }));

    let light_on_command = LightOnCommand::new(light.clone());
    let light_off_command = LightOffCommand::new(light.clone());

    remote_control.set_commands(0, Box::new(light_on_command), Box::new(light_off_command));
    remote_control.to_string();

    remote_control.on_pressing_on_button(0);
    remote_control.on_pressing_off_button(0);
    remote_control.on_pressing_undo_button();
}