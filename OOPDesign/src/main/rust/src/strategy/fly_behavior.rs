pub trait FlyBehavior {
    fn fly(&self);
}

pub struct FlyWithWings;
impl FlyBehavior for FlyWithWings {
    fn fly(&self) {
        println!("I'm flying with wings.");
    }
}

pub struct FlyWithRocket;
impl FlyBehavior for FlyWithRocket {
    fn fly(&self) {
        println!("I'm flying with rocket.");
    }
}

pub struct FlyNoWay;
impl FlyBehavior for FlyNoWay {
    fn fly(&self) {
        println!("I can't fly.");
    }
}