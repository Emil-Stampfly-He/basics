package strategy.duck.quack;

public class QuackNoWay implements QuackBehavior {
    @Override
    public void quack() {
        System.out.println("<< Silence >>");
    }
}
