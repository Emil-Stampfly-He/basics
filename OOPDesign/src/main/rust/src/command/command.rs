use std::cell::RefCell;
use std::rc::Rc;
use crate::command::furniture::{CeilingFan, Light, Speed};

pub trait Command {
    fn execute(&mut self);
    fn undo(&mut self);
    fn name(&self) -> &str;
    fn clone_box(&self) -> Box<dyn Command>;
}

impl Clone for Box<dyn Command> {
    fn clone(&self) -> Box<dyn Command> {
        self.clone_box()
    }
}

/// NoCommand
#[derive(Clone)]
pub struct NoCommand;
impl NoCommand {
    pub fn new() -> Self {
        Self {}
    }
}
impl Command for NoCommand {
    fn execute(&mut self) {}

    fn undo(&mut self) {}

    fn name(&self) -> &str {
        "NoCommand"
    }

    fn clone_box(&self) -> Box<dyn Command> {
        Box::new(self.clone())
    }
}

/// Light
/// LightOnCommand & LightOffCommand
/// LightOnCommand
#[derive(Clone)]
pub struct LightOnCommand {
    light: Rc<RefCell<Light>>
}

impl LightOnCommand {
    pub fn new(light: Rc<RefCell<Light>>) -> Self {
        Self { light }
    }
}

impl Command for LightOnCommand {
    fn execute(&mut self) {
        self.light.borrow_mut().on();
    }

    fn undo(&mut self) {
        self.light.borrow_mut().off();
    }

    fn name(&self) -> &str {
        "LightOnCommand"
    }

    fn clone_box(&self) -> Box<dyn Command> {
        Box::new(self.clone())
    }
}

/// LightOffCommand
#[derive(Clone)]
pub struct LightOffCommand {
    light: Rc<RefCell<Light>>,
}

impl LightOffCommand {
    pub fn new(light: Rc<RefCell<Light>>) -> Self {
        Self { light }
    }
}

impl Command for LightOffCommand {
    fn execute(&mut self) {
        self.light.borrow_mut().off();
    }

    fn undo(&mut self) {
        self.light.borrow_mut().on();
    }

    fn name(&self) -> &str {
        "LightOffCommand"
    }

    fn clone_box(&self) -> Box<dyn Command> {
        Box::new(self.clone())
    }
}

/// CeilingFan
/// CeilingFanHighCommand, CeilingFanMediumCommand, CeilingFanLowCommand & CeilingFanOffCommand
/// CeilingFanHighCommand
#[derive(Clone)]
pub struct CeilingFanHighCommand {
    ceiling_fan: Rc<RefCell<CeilingFan>>,
    prev_speed: Speed
}

impl CeilingFanHighCommand {
    pub fn new(ceiling_fan: Rc<RefCell<CeilingFan>>) -> Self {
        Self { ceiling_fan, prev_speed: Speed::Off }
    }
}

impl Command for CeilingFanHighCommand {
    fn execute(&mut self) {
        self.prev_speed = self.ceiling_fan.borrow().speed.clone();
        self.ceiling_fan.borrow_mut().high();
    }

    fn undo(&mut self) {
        self.ceiling_fan.borrow_mut().set_speed(&self.prev_speed);
    }

    fn name(&self) -> &str {
        "CeilingFanHighCommand"
    }

    fn clone_box(&self) -> Box<dyn Command> {
        Box::new(self.clone())
    }
}

#[derive(Clone)]
pub struct CeilingFanOffCommand {
    ceiling_fan: Rc<RefCell<CeilingFan>>,
    prev_speed: Speed
}

impl CeilingFanOffCommand {
    pub fn new(ceiling_fan: Rc<RefCell<CeilingFan>>) -> Self {
        Self { ceiling_fan, prev_speed: Speed::Off }
    }
}

impl Command for CeilingFanOffCommand {
    fn execute(&mut self) {
        self.prev_speed = self.ceiling_fan.borrow().speed.clone();
        self.ceiling_fan.borrow_mut().off();
    }

    fn undo(&mut self) {
        self.ceiling_fan.borrow_mut().set_speed(&self.prev_speed);
    }

    fn name(&self) -> &str {
        "CeilingFanOffCommand"
    }

    fn clone_box(&self) -> Box<dyn Command> {
        Box::new(self.clone())
    }
}

