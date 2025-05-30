package strategy.adv.weapon;

public class SwordBehavior implements WeaponBehavior {

    @Override
    public void useWeapon() {
        System.out.println("Use sword");
    }
}
