package decorator.starbuzz.condiment;

import decorator.starbuzz.beverage.Beverage;

public class Soy extends Condiment {
    public Soy(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return this.beverage.getDescription() + ", Soy";
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
