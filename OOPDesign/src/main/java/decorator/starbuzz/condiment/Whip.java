package decorator.starbuzz.condiment;

import decorator.starbuzz.beverage.Beverage;

public class Whip extends Condiment {
    public Whip(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return this.beverage.getDescription() + ", Whip";
    }

    @Override
    public double cost() {
        return switch (this.beverage.getSize()) {
            case VENTI -> this.beverage.cost() + 0.05;
            case GRANDE -> this.beverage.cost() + 0.10;
            case TALL -> this.beverage.cost() + 0.15;
        };
    }
}
