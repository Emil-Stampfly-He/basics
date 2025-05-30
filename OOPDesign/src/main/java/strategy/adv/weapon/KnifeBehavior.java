package strategy.adv.weapon;

public class KnifeBehavior implements WeaponBehavior {
    @Override
    public void useWeapon() {
        System.out.println("Use knife");
    }
}
