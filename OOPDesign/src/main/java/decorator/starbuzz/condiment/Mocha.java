package decorator.starbuzz.condiment;

import decorator.starbuzz.beverage.Beverage;

public class Mocha extends Condiment {
    public Mocha(Beverage beverage) {
        this.beverage = beverage;
    }

    @Override
    public String getDescription() {
        return this.beverage.getDescription() + ", Mocha";
    }

    @Override
    public double cost() {
        // 这个 + 0.20 即为“装饰”
        return switch (this.beverage.getSize()) {
            case VENTI -> this.beverage.cost() + 0.20;
            case GRANDE -> this.beverage.cost() + 0.40;
            case TALL -> this.beverage.cost() + 0.60;
        };
    }
}
