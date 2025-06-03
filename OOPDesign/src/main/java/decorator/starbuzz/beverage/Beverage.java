package decorator.starbuzz.beverage;

public abstract class Beverage {
    Size size = Size.TALL;
    String description = "Unknown Beverage";

    public String getDescription() {
        return this.description;
    }
    public void setSize(Size size) { this.size = size; }
    public Size getSize() { return size; }
    public abstract double cost();

    public enum Size { TALL, GRANDE, VENTI }
}
