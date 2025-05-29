package starter.adv.character;

import lombok.Setter;
import starter.adv.weapon.WeaponBehavior;

@Setter
public abstract class Character {

    WeaponBehavior weapon;

    public Character() {}

    public abstract void fight();

}
