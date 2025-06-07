package adapter.turkey;

import strategy.duck.Duck;
import strategy.duck.fly.FlyWithWings;
import strategy.duck.quack.Quack;

public class TurkeyAdapter extends Duck {
    Turkey turkey;

    public TurkeyAdapter(Turkey turkey) {
        this.quackBehavior = new Quack();
        this.flyBehavior = new FlyWithWings();
        this.turkey = turkey;
    }

    @Override
    public void performQuack() {
        this.turkey.gobble();
    }

    @Override
    public void performFly() {
        for (int i = 0; i < 5; i++) {
            this.turkey.fly();
        }
    }

    @Override
    public void display() {}
}
