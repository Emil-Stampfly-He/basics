package strategy.adv.character;

import lombok.Setter;
import strategy.adv.weapon.WeaponBehavior;

@Setter
public abstract class Character {

    WeaponBehavior weapon;

    public Character() {}

    public abstract void fight();

}
