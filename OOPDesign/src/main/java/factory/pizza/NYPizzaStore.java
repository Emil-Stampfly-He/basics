package factory.pizza;

import factory.ingredient.NYPizzaIngredientFactory;
import factory.ingredient.PizzaIngredientFactory;

public class NYPizzaStore extends PizzaStore {

    @Override
    public Pizza createPizza(String type) {
        PizzaIngredientFactory ingredientFactory = new NYPizzaIngredientFactory();

        return switch (type) {
            case "cheese" -> new CheesePizza(ingredientFactory);
            case "pepperoni" -> new PepperoniPizza(ingredientFactory);
            case "clam" -> new ClamPizza(ingredientFactory);
            default -> null;
        };
    }
}
