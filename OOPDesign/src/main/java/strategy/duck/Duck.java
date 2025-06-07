package strategy.duck;

import strategy.duck.fly.FlyBehavior;
import strategy.duck.quack.QuackBehavior;

public abstract class Duck {
    public QuackBehavior quackBehavior;
    public FlyBehavior flyBehavior;

    public Duck() {}

    public abstract void display();

    public void performQuack() {
        this.quackBehavior.quack();
    }

    public void performFly() {
        this.flyBehavior.fly();
    }

    public void swim() {
        System.out.println("All ducks can swim.");
    }
}
