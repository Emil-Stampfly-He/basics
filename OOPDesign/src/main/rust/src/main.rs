mod strategy;

use std::io::{BufReader, Cursor, Read};
use rust::adapter::adaptee::SpecificTarget;
use rust::adapter::adapter::TargetAdapter;
use rust::adapter::target::{OrdinaryTarget, Target};
use rust::command::client::run_client;
use rust::command::enum_match::run_enum_match_pattern;
use rust::decorator::beverage::run_starbuzz;
use crate::strategy::duck_with_generics::DuckWithGenerics;
use crate::strategy::duck_with_trait::Duck;
use crate::strategy::fly_behavior::{FlyNoWay, FlyWithRocket, FlyWithWings};
use crate::strategy::quack_behavior::{Quack, Squeak};

fn main() {
    println!("----- Strategy Pattern -----");
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
    
    println!();
    println!("----- Command Pattern -----");
    
    run_client();
    println!();
    run_enum_match_pattern();

    println!();
    println!("----- Decorator Pattern -----");
    
    let mut buf = [0u8; 10];
    // BufReader为装饰者
    let mut input = BufReader::new(Cursor::new("input data"));
    
    input.read(&mut buf).ok();
    print!("Read from a buffered reader: ");
    
    for byte in buf {
        print!("{}", char::from(byte));
    }
    println!();
    
    run_starbuzz();

    println!();
    println!("----- Adapter Pattern -----");
    
    let target = OrdinaryTarget;
    println!("A compatible target can be directly called");
    call(target);
    
    let adaptee = SpecificTarget;
    println!("Adaptee is incompatible with client: {}", adaptee.specific_request());
    
    let adapter = TargetAdapter::new(adaptee);
    println!("With adapter client can call its method");
    call(adapter);
}

fn call(target: impl Target) {
    println!("'{}'", target.request());
}
