use crate::command::furniture::Light;

pub enum Command<'a> {
    LightOn { light: &'a mut Light },
    LightOff { light: &'a mut Light },
}

impl<'a> Command<'a> {
    fn execute(self) {
        match self {
            Command::LightOn{ light } => light.on(),
            Command::LightOff{ light } => light.off(),
        }
    }
}

pub fn run_enum_match_pattern() {
    let mut light = Light { on: false};
    {
        let cmd1 = Command::LightOn  { light: &mut light };
        cmd1.execute();  // 执行完就把 &mut light 还回去
    }
    {
        let cmd2 = Command::LightOff { light: &mut light };
        cmd2.execute();
    }
}