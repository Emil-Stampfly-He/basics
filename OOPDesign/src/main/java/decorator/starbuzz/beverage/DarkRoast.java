package decorator.starbuzz.beverage;

public class DarkRoast extends Beverage {
    public DarkRoast() {
        this.description = "Dark Roast";
    }

    @Override
    public double cost() {
        return 0.99;
    }
}
