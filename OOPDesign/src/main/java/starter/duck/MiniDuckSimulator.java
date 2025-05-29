package starter.duck;

public class MiniDuckSimulator {
    public static void main(String[] args) {
        Duck mallardDuck = new MallardDuck();
        mallardDuck.performFly();
        mallardDuck.performQuack();
        mallardDuck.swim();

        Duck duckCall = new DuckCall();
        duckCall.performFly();
        duckCall.performQuack();
        duckCall.swim();
    }
}
