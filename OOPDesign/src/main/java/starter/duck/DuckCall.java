package starter.duck;

import starter.duck.fly.FlyNoWay;
import starter.duck.quack.Quack;

public class DuckCall extends Duck {

    public DuckCall() {
        this.flyBehavior = new FlyNoWay();
        this.quackBehavior = new Quack();
    }

    @Override
    public void display() {
        System.out.println("Duck Call");
    }
}
