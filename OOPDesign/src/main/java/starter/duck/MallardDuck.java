package starter.duck;

import starter.duck.fly.FlyWithWings;
import starter.duck.quack.Quack;

public class MallardDuck extends Duck {

    public MallardDuck() {
        this.quackBehavior = new Quack();
        this.flyBehavior = new FlyWithWings();
    }

    @Override
    public void display() {
        System.out.println("Mallard Duck");
    }
}
