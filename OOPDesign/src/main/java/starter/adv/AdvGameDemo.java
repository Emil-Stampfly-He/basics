package starter.adv;

import starter.adv.character.Character;
import starter.adv.character.King;
import starter.adv.character.Queen;
import starter.adv.character.Troll;
import starter.adv.weapon.AxeBehavior;
import starter.adv.weapon.BowAndArrowBehavior;
import starter.adv.weapon.KnifeBehavior;

public class AdvGameDemo {
    public static void main(String[] args) {
        Character king = new King();
        king.setWeapon(new AxeBehavior());
        king.fight();

        Character queen = new Queen();
        queen.setWeapon(new KnifeBehavior());
        queen.fight();

        Character troll = new Troll();
        troll.setWeapon(new BowAndArrowBehavior());
        troll.fight();
    }
}
