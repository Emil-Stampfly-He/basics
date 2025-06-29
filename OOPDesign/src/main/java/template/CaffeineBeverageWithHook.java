package template;

public abstract class CaffeineBeverageWithHook {
    // 模板方法
    final void prepareRecipe(){
        boilWater();
        brew();
        pourInCup();
        if (customerWantsCondiments()) {
            addCondiments();
        }
    }

    // 应当被改写的方法
    abstract void addCondiments();

    abstract void brew();

    void boilWater(){
        System.out.println("Boil water");
    }

    void pourInCup(){
        System.out.println("Pour in cup");
    }

    boolean customerWantsCondiments(){
        return true;
    }
}
