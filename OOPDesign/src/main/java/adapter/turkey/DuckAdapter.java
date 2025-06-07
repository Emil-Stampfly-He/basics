package adapter.turkey;

import strategy.duck.Duck;

import java.util.Random;

public class DuckAdapter implements Turkey {
    Duck duck;
    Random random;

    public DuckAdapter(Duck duck) {
        this.duck = duck;
        this.random = new Random();
    }

    @Override
    public void gobble() {
        this.duck.performQuack();
    }

    @Override
    public void fly() {
        if (this.random.nextInt(5) == 0) {
            this.duck.performFly();
        }
    }
}
