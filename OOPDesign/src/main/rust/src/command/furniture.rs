pub struct Light {
    pub on: bool,
}

impl Light {
    pub fn new() -> Self {
        Self { on: false }
    }

    pub fn on(&mut self) {
        self.on = true;
        println!("Light is on");
    }

    pub fn off(&mut self) {
        self.on = false;
        println!("Light is off");
    }
}

#[derive(Clone)]
pub enum Speed {
    Off,
    Low,
    Medium,
    High,
}

pub struct CeilingFan {
    pub speed: Speed,
}

impl CeilingFan {
    pub fn new() -> Self {
        Self { speed: Speed::Off }
    }

    pub fn high(&mut self) {
        self.speed = Speed::High;
        println!("CeilingFan is on high");
    }

    pub fn medium(&mut self) {
        self.speed = Speed::Medium;
        println!("CeilingFan is on medium");
    }

    pub fn low(&mut self) {
        self.speed = Speed::Low;
        println!("CeilingFan is on low");
    }

    pub fn off(&mut self) {
        self.speed = Speed::Off;
        println!("CeilingFan is off");
    }

    pub fn get_speed(&self) -> usize {
        match self.speed {
            Speed::Off => 0,
            Speed::Low => 1,
            Speed::Medium => 2,
            Speed::High => 3,
        }
    }

    pub fn set_speed(&mut self, speed: &Speed) {
        match speed {
            Speed::Off => self.off(),
            Speed::Low => self.low(),
            Speed::Medium => self.medium(),
            Speed::High => self.high(),
        }
    }
}

