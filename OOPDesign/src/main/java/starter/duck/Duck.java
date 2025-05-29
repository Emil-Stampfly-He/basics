package starter.duck;

import starter.duck.fly.FlyBehavior;
import starter.duck.quack.QuackBehavior;

public abstract class Duck {
    QuackBehavior quackBehavior;
    FlyBehavior flyBehavior;

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
