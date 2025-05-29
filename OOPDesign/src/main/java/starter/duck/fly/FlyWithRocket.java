package starter.duck.fly;

public class FlyWithRocket implements FlyBehavior {
    @Override
    public void fly() {
        System.out.println("I'm flying with Rocket.");
    }
}
