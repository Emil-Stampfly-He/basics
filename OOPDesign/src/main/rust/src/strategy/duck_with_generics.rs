use crate::strategy::fly_behavior::FlyBehavior;
use crate::strategy::quack_behavior::QuackBehavior;

pub struct DuckWithGenerics<Q: QuackBehavior, F: FlyBehavior> {
    pub quack_behavior: Q,
    pub fly_behavior: F,
}

impl<Q, F> DuckWithGenerics<Q, F>
where
    Q: QuackBehavior,
    F: FlyBehavior
{
    pub fn new(quack: Q, fly: F) -> Self {
        DuckWithGenerics {
            quack_behavior: quack,
            fly_behavior: fly,
        }
    }
    
    pub fn perform_quack(&self) {
        self.quack_behavior.quack();  
    }
    
    pub fn perform_fly(&self) {
        self.fly_behavior.fly();  
    }
    
    pub fn swim(&self) {
        println!("All ducks float, even decoys!");   
    }
}