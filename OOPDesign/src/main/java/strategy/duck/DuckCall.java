package strategy.duck;

import strategy.duck.fly.FlyNoWay;
import strategy.duck.quack.Quack;

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
