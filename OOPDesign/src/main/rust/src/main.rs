mod strategy;
use crate::strategy::duck_with_generics::DuckWithGenerics;
use crate::strategy::duck_with_trait::Duck;
use crate::strategy::fly_behavior::{FlyNoWay, FlyWithRocket, FlyWithWings};
use crate::strategy::quack_behavior::{Quack, Squeak};

fn main() {
    let mallard_duck = DuckWithGenerics::new(
        Quack,
        FlyWithWings,
    );
    
    mallard_duck.perform_fly();
    mallard_duck.perform_quack();
    
    let mut ducks = vec![];
    let rubber_duck = Duck::new(Box::new(Squeak), Box::new(FlyNoWay));
    let duck_call = Duck::new(Box::new(Quack), Box::new(FlyNoWay));
    let red_head_duck = Duck::new(Box::new(Quack), Box::new(FlyWithWings));
    let rocket_duck = Duck::new(Box::new(Quack), Box::new(FlyWithRocket));
    ducks.push(rubber_duck);
    ducks.push(duck_call);
    ducks.push(red_head_duck);
    ducks.push(rocket_duck);
    for duck in ducks {
        duck.perform_fly();
        duck.perform_quack();
    }
    
    let mut mallard_duck = Duck::new(Box::new(Quack), Box::new(FlyWithWings));
    mallard_duck.set_quack_behavior(Box::new(Squeak));
}
