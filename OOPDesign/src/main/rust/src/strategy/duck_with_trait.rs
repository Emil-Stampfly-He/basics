use crate::strategy::fly_behavior::FlyBehavior;
use crate::strategy::quack_behavior::QuackBehavior;

pub struct Duck {
    quack_behavior: Box<dyn QuackBehavior>,
    fly_behavior: Box<dyn FlyBehavior>,
}

impl Duck {
    pub fn new(
        quack: Box<dyn QuackBehavior>, 
        fly: Box<dyn FlyBehavior>) -> Self {
        Self { quack_behavior: quack, fly_behavior: fly }
    }
    
    pub fn perform_quack(&self) { self.quack_behavior.quack(); }
    pub fn perform_fly(&self) { self.fly_behavior.fly(); }
    
    pub fn set_fly_behavior(&mut self, fly: Box<dyn FlyBehavior>) { self.fly_behavior = fly; }
    pub fn set_quack_behavior(&mut self, quack: Box<dyn QuackBehavior>) { self.quack_behavior = quack; }
}