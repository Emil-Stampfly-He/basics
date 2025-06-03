package decorator.starbuzz;

import decorator.starbuzz.beverage.Beverage;
import decorator.starbuzz.beverage.DarkRoast;
import decorator.starbuzz.beverage.Espresso;
import decorator.starbuzz.beverage.HouseBlend;
import decorator.starbuzz.condiment.Mocha;
import decorator.starbuzz.condiment.Soy;
import decorator.starbuzz.condiment.Whip;

public class StarbuzzCoffee {
    public static void main(String[] args) {
        Beverage espresso = new Espresso();
        System.out.println(espresso.getDescription() + " $" + espresso.cost());

        Beverage darkRoast = new DarkRoast();
        // 使用Mocha装饰（包裹）
        darkRoast = new Mocha(darkRoast);
        // 再使用Mocha装饰（包裹）
        darkRoast = new Mocha(darkRoast);
        // 使用Whip装饰（包裹）
        darkRoast = new Whip(darkRoast);
        System.out.println(darkRoast.getDescription() + " $" + darkRoast.cost());

        Beverage houseBlend = new HouseBlend();
        houseBlend = new Soy(houseBlend);
        houseBlend = new Whip(houseBlend);
        houseBlend = new Mocha(houseBlend);
        System.out.println(houseBlend.getDescription() + " $" + houseBlend.cost());
    }
}
