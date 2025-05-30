package strategy.adv;

import strategy.adv.character.Character;
import strategy.adv.character.King;
import strategy.adv.character.Queen;
import strategy.adv.character.Troll;
import strategy.adv.weapon.AxeBehavior;
import strategy.adv.weapon.BowAndArrowBehavior;
import strategy.adv.weapon.KnifeBehavior;

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
