use std::cell::RefCell;
use std::rc::Rc;
use crate::command::command::{CeilingFanHighCommand, CeilingFanOffCommand, LightOffCommand, LightOnCommand};
use crate::command::furniture::{CeilingFan, Light, Speed};
use crate::command::remote_control::RemoteControl;

pub fn run_client() {
    let mut remote_control = RemoteControl::new();

    let light = Rc::new(RefCell::new(Light {
        on: false,
    }));
    let light_on_command = LightOnCommand::new(light.clone());
    let light_off_command = LightOffCommand::new(light.clone());

    let ceiling_fan = Rc::new(RefCell::new(CeilingFan {
        speed: Speed::Off,
    }));
    let ceiling_on_high_command = CeilingFanHighCommand::new(ceiling_fan.clone());
    let ceiling_off_command = CeilingFanOffCommand::new(ceiling_fan.clone());

    remote_control.set_commands(0, Box::new(light_on_command), Box::new(light_off_command));
    remote_control.set_commands(1, Box::new(ceiling_on_high_command), Box::new(ceiling_off_command));
    remote_control.to_string();

    remote_control.on_pressing_on_button(0);
    remote_control.on_pressing_off_button(0);
    remote_control.on_pressing_undo_button();

    remote_control.on_pressing_on_button(1);
    remote_control.on_pressing_off_button(1);
    remote_control.on_pressing_undo_button();
}