#[derive(Clone, Copy)]
pub enum Size { Tall, Grande, Venti }

pub trait Beverage {
    fn get_description(&self) -> String;
    fn set_size(&mut self, size: Size);
    fn get_size(&self) -> Size;
    fn cost(&self) -> f64;
}

pub struct DarkRoast { size: Size, }

impl DarkRoast {
    pub fn new(size: Size) -> DarkRoast {
        DarkRoast { size }
    }
}

impl Beverage for DarkRoast {
    fn get_description(&self) -> String {
        "Dark Roast".to_string()
    }

    fn set_size(&mut self, size: Size) {
        self.size = size;
    }

    fn get_size(&self) -> Size {
        self.size.clone()
    }

    fn cost(&self) -> f64 {
        0.99
    }
}

pub struct Mocha<B: Beverage> {
    beverage: B,
}

impl<B: Beverage> Mocha<B> {
    pub fn new(beverage: B) -> Self {
        Mocha { beverage }
    }
}

impl<B: Beverage> Beverage for Mocha<B> {
    fn get_description(&self) -> String {
        format!("Mocha {}", self.beverage.get_description())
    }

    fn set_size(&mut self, size: Size) {
        self.beverage.set_size(size);
    }

    fn get_size(&self) -> Size {
        self.beverage.get_size()
    }

    fn cost(&self) -> f64 {
        let base_cost = self.beverage.cost();
        match self.beverage.get_size() {
            Size::Tall => base_cost + 0.20,
            Size::Grande => base_cost + 0.40,
            Size::Venti => base_cost + 0.60,
        }
    }
}
