package template;

public class Tea extends CaffeineBeverageWithHook {
    @Override
    public void addCondiments() {
        System.out.println("Adding lemon");
    }

    @Override
    public void brew() {
        System.out.println("Steeping the tea");
    }
}
