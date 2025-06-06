package factory.pizza;

public abstract class Pizza {
    public String pizzaName;

    protected abstract void prepare();
    protected abstract void bake();
    protected abstract void cut();
    protected abstract void box();
}
