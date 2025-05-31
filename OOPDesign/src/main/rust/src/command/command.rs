use std::cell::RefCell;
use std::rc::Rc;
use crate::command::furniture::Light;

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

