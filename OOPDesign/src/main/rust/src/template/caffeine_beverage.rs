use std::io;
use std::io::Write;

pub trait CaffeineBeverage {
    fn prepare_recipe() {
        Self::boil_water();
        Self::brew();
        Self::pour_in_cup();
        
        if Self::customer_wants_condiments() { 
            Self::add_condiments();
        }
    }
    
    fn boil_water() {
        println!("Boil water");
    }
    
    fn brew();
    
    fn add_condiments();
    
    fn pour_in_cup() {
        println!("Pour in cup");
    }
    
    fn customer_wants_condiments() -> bool {
        true
    }
}

pub struct Coffee;
impl CaffeineBeverage for Coffee {
    fn brew() {
        println!("Brew coffee")
    }

    fn add_condiments() {
        println!("Add sugar and milk")
    }

    fn customer_wants_condiments() -> bool {
        let answer = get_user_input();
        answer.to_lowercase().starts_with('y')
    }
}

pub struct Tea;
impl CaffeineBeverage for Tea {
    fn brew() {
        println!("Brew tea")
    }
    
    fn add_condiments() {
        println!("Add milk")
    }
}

fn get_user_input() -> String {
    println!("Would you like milk and sugar with you coffee (y/n)? ");
    io::stdout().flush().expect("Failed to flush stdout");

    let mut input = String::new();

    match io::stdin().read_line(&mut input) {
        Ok(_) => {
            input.trim().to_lowercase()
        }
        Err(e) => {
            eprintln!("Error: {}", e);
            "no".to_string()
        }
    }
}