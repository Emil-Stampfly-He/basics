package decorator.starbuzz.condiment;

import decorator.starbuzz.beverage.Beverage;

public abstract class Condiment extends Beverage {
    // 被装饰（包裹）对象
    Beverage beverage;

    public abstract String getDescription();
}
