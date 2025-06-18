use crate::adapter::adaptee::SpecificTarget;
use crate::adapter::target::Target;

pub struct TargetAdapter {
    adaptee: SpecificTarget,
}

impl TargetAdapter {
    pub fn new(adapter: SpecificTarget) -> TargetAdapter {
        TargetAdapter { adaptee: adapter }
    }
}

impl Target for TargetAdapter {
    fn request(&self) -> String {
        self.adaptee.specific_request()
            .chars()
            .rev()
            .collect::<String>()
    }
}

