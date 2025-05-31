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

