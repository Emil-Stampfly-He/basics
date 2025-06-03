package decorator.starbuzz.condiment;

import decorator.starbuzz.beverage.Beverage;

public class SteamedMilk extends Condiment {
    public SteamedMilk(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return this.beverage.getDescription() + ", Steamed Milk";
    }

    @Override
    public double cost() {
        return switch (this.beverage.getSize()) {
            case VENTI -> this.beverage.cost() + 0.10;
            case GRANDE -> this.beverage.cost() + 0.15;
            case TALL -> this.beverage.cost() + 0.20;
        };
    }
}
